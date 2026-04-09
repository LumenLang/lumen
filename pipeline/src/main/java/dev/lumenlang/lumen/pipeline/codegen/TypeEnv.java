package dev.lumenlang.lumen.pipeline.codegen;

import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.type.RefTypeHandle;
import dev.lumenlang.lumen.pipeline.data.DataSchema;
import dev.lumenlang.lumen.pipeline.persist.GlobalVars;
import dev.lumenlang.lumen.pipeline.persist.PersistentVars;
import dev.lumenlang.lumen.pipeline.placeholder.PlaceholderExpander;
import dev.lumenlang.lumen.pipeline.type.LumenType;
import dev.lumenlang.lumen.pipeline.var.RefType;
import dev.lumenlang.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The compile-time symbol table for a Lumen script.
 *
 * <p>{@code TypeEnv} tracks what variable names are in scope and what Java expressions they
 * correspond to. It does <em>not</em> track runtime values, all information here is used
 * solely to generate correct Java source code.
 *
 * <h2>Scope Stack</h2>
 * <p>Scope is managed through a stack of {@link BlockContext} objects. Each time a block begins
 * (e.g. {@code on join:}), a new {@link BlockContext} is pushed via {@link #enterBlock(BlockContext)}.
 * When the block ends it is popped via {@link #leaveBlock()}. Lookups via
 * {@link #lookupVar(String)} walk the stack from innermost to outermost scope.
 *
 * <h2>Globals</h2>
 * <p>Block handlers can store arbitrary data across their {@code begin} and {@code end} calls
 * using {@link #put(String, Object)} and {@link #get(String)}. These globals are not scoped.
 *
 * @see BlockContext
 * @see VarRef
 */
@SuppressWarnings("unused")
public final class TypeEnv implements EnvironmentAccess {
    private final Map<String, Object> globals = new HashMap<>();
    private final Map<String, String> storedKeys = new HashMap<>();
    private final Map<String, String> storedBaseKeys = new HashMap<>();
    private final Map<String, String> storedScopeVars = new HashMap<>();
    private final Set<String> runtimeGlobals = new HashSet<>();
    private final Set<String> globalFields = new HashSet<>();
    private final Set<String> experimental = new HashSet<>();
    private final List<GlobalVarInfo> globalVars = new ArrayList<>();
    private final List<ConfigEntry> configEntries = new ArrayList<>();
    private final Map<String, VarRef> rootVars = new HashMap<>();
    private final Map<String, DataSchema> dataSchemas = new HashMap<>();
    private final Map<String, NullState> nullStates = new HashMap<>();
    private final Map<String, NullableVarInfo> nullableVarInfos = new HashMap<>();
    private BlockContext currentBlock;

    /**
     * Tracks whether a nullable variable is currently known to be null or non-null.
     *
     * <p>This is a definite state, not a probability. {@code NULL} means the variable
     * is certainly null at this point in the code (declared without default, or last reassigned to none).
     * {@code NON_NULL} means a concrete value was provided (default value or reassignment to a value).
     */
    public enum NullState {
        NULL,
        NON_NULL
    }

    /**
     * Records the declaration site of a nullable variable for use in multi-line diagnostics.
     *
     * @param declarationLine the line where the variable was declared
     * @param declarationRaw  the raw source text of the declaration line
     */
    public record NullableVarInfo(int declarationLine, @NotNull String declarationRaw) {
    }

    /**
     * Sets the current null state for a nullable variable.
     *
     * @param name  the variable name
     * @param state the null state
     */
    public void markNullState(@NotNull String name, @NotNull NullState state) {
        nullStates.put(name, state);
    }

    /**
     * Returns the current null state of a variable, or {@code null} if not tracked.
     *
     * @param name the variable name
     * @return the null state, or {@code null}
     */
    public @Nullable NullState nullState(@NotNull String name) {
        return nullStates.get(name);
    }

    /**
     * Records the declaration site of a nullable variable.
     *
     * @param name the variable name
     * @param info the declaration info
     */
    public void recordNullableVarInfo(@NotNull String name, @NotNull NullableVarInfo info) {
        nullableVarInfos.put(name, info);
    }

    /**
     * Returns the declaration site info for a nullable variable, or {@code null} if not recorded.
     *
     * @param name the variable name
     * @return the declaration info, or {@code null}
     */
    public @Nullable NullableVarInfo nullableVarInfo(@NotNull String name) {
        return nullableVarInfos.get(name);
    }

    /**
     * Pushes a new {@link BlockContext} onto the scope stack, making it the active scope.
     *
     * @param ctx the new block context to enter
     */
    public void enterBlock(BlockContext ctx) {
        this.currentBlock = ctx;
    }

    /**
     * Pops the current {@link BlockContext} from the scope stack, restoring the parent scope.
     *
     * <p>Does nothing if the stack is already empty.
     */
    public void leaveBlock() {
        if (currentBlock != null)
            this.currentBlock = currentBlock.parent();
    }

    /**
     * Returns the currently active {@link BlockContext}, or {@code null} if not inside any block.
     *
     * @return the current block context
     */
    public BlockContext blockContext() {
        return currentBlock;
    }

    @Override
    public BlockContext block() {
        return currentBlock;
    }

    /**
     * Defines a named {@link VarRef} in the current block scope.
     *
     * @param name the variable name to bind
     * @param var  the variable descriptor
     */
    public void defineVar(String name, VarRef var) {
        currentBlock.defineVar(name, var);
    }

    /**
     * Looks up a named {@link VarRef} by walking the scope stack from innermost to outermost.
     *
     * @param name the variable name to look up
     * @return the {@link VarRef} if found, or {@code null}
     */
    @Override
    public VarRef lookupVar(@NotNull String name) {
        for (BlockContext c = currentBlock; c != null; c = c.parent()) {
            VarRef v = c.getVarLocal(name);
            if (v != null) return v;
        }
        return rootVars.get(name);
    }

    /**
     * Defines a variable at the root (class) scope, making it visible from all block contexts.
     *
     * <p>This is used for config entries that become class-level fields in the generated Java class.
     *
     * @param name the variable name
     * @param var  the variable descriptor
     */
    public void defineRootVar(@NotNull String name, @NotNull VarRef var) {
        rootVars.put(name, var);
    }

    /**
     * Registers a data class schema, making it available for constructor and field access patterns.
     *
     * <p>Schemas are stored in the global map under the key {@code "data_schema_<name>"}.
     *
     * @param schema the data schema to register
     */
    public void registerDataSchema(@NotNull DataSchema schema) {
        dataSchemas.put(schema.name().toLowerCase(), schema);
        globals.put("data_schema_" + schema.name().toLowerCase(), schema);
    }

    /**
     * Returns the data schema for the given type name, or null if not defined.
     *
     * @param name the data type name (case-insensitive)
     * @return the schema, or null
     */
    public @Nullable DataSchema lookupDataSchema(@NotNull String name) {
        return dataSchemas.get(name.toLowerCase());
    }

    /**
     * Returns the first {@link VarRef} in scope whose {@link RefType} matches the given type.
     *
     * <p>Walks the scope stack from innermost to outermost scope, examining all variables
     * in each frame.
     *
     * @param type the ref type to match against
     * @return the first matching variable, or {@code null} if none found
     */
    public @Nullable VarRef lookupVarByType(@NotNull RefType type) {
        for (BlockContext c = currentBlock; c != null; c = c.parent()) {
            VarRef found = c.findVarByType(type);
            if (found != null) return found;
        }
        return null;
    }

    @Override
    public @Nullable VarRef lookupVarByType(@NotNull RefTypeHandle type) {
        RefType internal = type instanceof RefType rt ? rt : RefType.byId(type.id());
        if (internal == null) return null;
        return lookupVarByType(internal);
    }

    /**
     * Stores an arbitrary value in the global map, keyed by {@code key}.
     *
     * <p>Use this to pass data from a block handler's {@code begin} to its {@code end}.
     *
     * @param key   the key
     * @param value the value to store
     */
    @Override
    public void put(@NotNull String key, @NotNull Object value) {
        globals.put(key, value);
    }

    /**
     * Retrieves a previously stored global value, casting it to the inferred type.
     *
     * @param key the key
     * @param <T> the expected type
     * @return the stored value, or {@code null} if absent
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(@NotNull String key) {
        return (T) globals.get(key);
    }

    /**
     * Returns {@code true} if a value has been stored for the given key.
     *
     * @param key the key to test
     * @return {@code true} if present
     */
    public boolean has(@NotNull String key) {
        return globals.containsKey(key);
    }

    /**
     * Marks the given feature name as used, enabling experimental code paths for this script.
     *
     * @param feature the experimental feature identifier
     */
    public void useExperimental(@NotNull String feature) {
        experimental.add(feature);
    }

    /**
     * Returns {@code true} if the given experimental feature has been activated for this script.
     *
     * @param feature the experimental feature identifier
     * @return {@code true} if activated
     */
    public boolean isExperimental(@NotNull String feature) {
        return experimental.contains(feature);
    }

    /**
     * Returns an unmodifiable view of all experimental feature names that have been activated.
     *
     * @return the set of activated experimental feature identifiers
     */
    public Set<String> usedExperimental() {
        return Collections.unmodifiableSet(experimental);
    }

    /**
     * Marks a variable as stored (persistent), associating it with a storage key.
     *
     * <p>Stored variables are auto-saved when modified by math operations.
     *
     * @param name the variable name
     * @param key  the storage key expression
     */
    public void markStored(@NotNull String name, @NotNull String key) {
        storedKeys.put(name, key);
    }

    /**
     * Returns whether the named variable is a stored (persistent) variable.
     *
     * @param name the variable name
     * @return {@code true} if the variable was declared with {@code store}
     */
    @Override
    public boolean isStored(@NotNull String name) {
        return storedKeys.containsKey(name);
    }

    /**
     * Returns the storage key for a stored variable, or {@code null} if not stored.
     *
     * @param name the variable name
     * @return the storage key, or {@code null}
     */
    @Override
    public @Nullable String getStoredKey(@NotNull String name) {
        return storedKeys.get(name);
    }

    /**
     * Returns the base key prefix for a stored variable, or {@code null} if not tracked.
     *
     * @param name the variable name
     * @return the base key prefix, or {@code null}
     */
    @Override
    public @Nullable String getStoredBaseKey(@NotNull String name) {
        return storedBaseKeys.get(name);
    }

    /**
     * Returns the scope variable name for a stored variable, or {@code null} if unscoped.
     *
     * @param name the variable name
     * @return the scope variable name, or {@code null}
     */
    @Override
    public @Nullable String getStoredScopeVar(@NotNull String name) {
        return storedScopeVars.get(name);
    }

    /**
     * Returns whether the named variable is a runtime (non-persistent) global.
     *
     * @param name the variable name
     * @return {@code true} if declared as a non-stored global
     */
    public boolean isRuntimeGlobal(@NotNull String name) {
        return runtimeGlobals.contains(name);
    }

    @Override
    public @NotNull String storedClassName(@NotNull String name) {
        if (runtimeGlobals.contains(name)) return GlobalVars.class.getSimpleName();
        return PersistentVars.class.getSimpleName();
    }

    /**
     * Returns an unmodifiable list of all registered global variable declarations.
     *
     * @return the global var declarations
     */
    public @NotNull List<GlobalVarInfo> globalVars() {
        return Collections.unmodifiableList(globalVars);
    }

    /**
     * Returns {@code true} if a global variable with the given name has been declared.
     *
     * @param name the variable name
     * @return {@code true} if declared as a global
     */
    @Override
    public boolean isGlobal(@NotNull String name) {
        for (GlobalVarInfo g : globalVars) {
            if (g.name.equals(name)) return true;
        }
        return false;
    }

    @Override
    public @Nullable GlobalVarInfo getGlobalInfo(@NotNull String name) {
        for (GlobalVarInfo g : globalVars) {
            if (g.name.equals(name)) return g;
        }
        return null;
    }

    @Override
    public void registerGlobal(@NotNull GlobalInfo info) {
        if (info instanceof GlobalVarInfo gvi) {
            globalVars.add(gvi);
        } else {
            globalVars.add(new GlobalVarInfo(
                    info.name(), info.defaultJava(), info.className(),
                    info.scoped(), info.exprRefTypeId(), info.stored(),
                    info.exprMetadata()));
        }
    }

    @Override
    public @NotNull List<GlobalVarInfo> allGlobals() {
        return Collections.unmodifiableList(globalVars);
    }

    @Override
    public void markStored(@NotNull String name, @NotNull String keyExpr,
                           @NotNull String baseKey, @Nullable String scopeVar) {
        storedKeys.put(name, keyExpr);
        storedBaseKeys.put(name, baseKey);
        if (scopeVar != null) storedScopeVars.put(name, scopeVar);
    }

    @Override
    public void markRuntimeGlobal(@NotNull String name) {
        runtimeGlobals.add(name);
    }

    @Override
    public void markGlobalField(@NotNull String name) {
        globalFields.add(name);
    }

    @Override
    public boolean isGlobalField(@NotNull String name) {
        return globalFields.contains(name);
    }

    @Override
    public boolean isVarCapturedByLambda(@NotNull String name) {
        boolean passedLambdaBoundary = false;
        for (BlockContext c = currentBlock; c != null; c = c.parent()) {
            VarRef local = c.getVarLocal(name);
            if (!passedLambdaBoundary) {
                if (local != null) return false;
                Object marker = c.getEnv("__lambda_block");
                if (marker != null) passedLambdaBoundary = true;
            } else {
                if (local != null) return true;
            }
        }
        if (rootVars.containsKey(name)) return passedLambdaBoundary;
        return false;
    }

    @Override
    public void registerConfig(@NotNull String name, @NotNull String javaValue) {
        configEntries.add(new ConfigEntry(name, javaValue));
    }

    @Override
    public VarHandle defineRootVar(@NotNull String name, @Nullable RefTypeHandle refType, @NotNull String java) {
        RefType internal = refType instanceof RefType rt ? rt : (refType != null ? RefType.byId(refType.id()) : null);
        VarRef ref = new VarRef(internal, java);
        rootVars.put(name, ref);
        return ref;
    }

    /**
     * Registers a config entry from the script's {@code config:} block.
     *
     * @param entry the config entry
     */
    public void registerConfig(@NotNull ConfigEntry entry) {
        configEntries.add(entry);
    }

    /**
     * Returns an unmodifiable list of all registered config entries.
     *
     * @return the config entries
     */
    public @NotNull List<ConfigEntry> configEntries() {
        return Collections.unmodifiableList(configEntries);
    }

    @Override
    public VarHandle defineVar(@NotNull String name, @Nullable RefTypeHandle refType, @NotNull String java) {
        RefType internal = refType instanceof RefType rt ? rt : (refType != null ? RefType.byId(refType.id()) : null);
        VarRef ref = new VarRef(internal, java);
        defineVar(name, ref);
        return ref;
    }

    @Override
    public VarHandle defineVar(@NotNull String name, @Nullable RefTypeHandle refType,
                               @NotNull String java, @NotNull Map<String, Object> metadata) {
        RefType internal = refType instanceof RefType rt ? rt : (refType != null ? RefType.byId(refType.id()) : null);
        VarRef ref = new VarRef(internal, java, metadata);
        defineVar(name, ref);
        return ref;
    }

    /**
     * Defines a named variable in the current block scope with a full compile-time type.
     *
     * @param name      the variable name
     * @param refType   the ref type for type checking, or {@code null}
     * @param java      the Java variable name
     * @param lumenType the full compile-time type, or {@code null}
     * @param metadata  compile-time metadata entries
     */
    public void defineVar(@NotNull String name, @Nullable RefType refType,
                          @NotNull String java, @Nullable LumenType lumenType,
                          @NotNull Map<String, Object> metadata) {
        VarRef ref = new VarRef(refType, java, lumenType, metadata);
        defineVar(name, ref);
    }

    @Override
    public @NotNull String expandPlaceholders(@NotNull String raw) {
        return PlaceholderExpander.expand(raw, this);
    }

    @Override
    public @NotNull String persistClassName() {
        return PersistentVars.class.getSimpleName();
    }

    /**
     * Information about a script-wide variable declared with {@code global}.
     *
     * @param name          the variable name
     * @param defaultJava   the Java expression for the default value
     * @param className     the script class name at the time of declaration
     * @param scoped        whether the global is scoped per entity rather than server-wide
     * @param exprRefTypeId the optional expression ref type id
     * @param stored        whether the variable is persisted to disk ({@code true}) or in-memory only ({@code false})
     * @param exprMetadata  compile-time metadata from the default expression, or {@code null}
     */
    public record GlobalVarInfo(@NotNull String name, @NotNull String defaultJava,
                                @NotNull String className, boolean scoped,
                                @Nullable String exprRefTypeId, boolean stored,
                                @Nullable Map<String, Object> exprMetadata)
            implements EnvironmentAccess.GlobalInfo {
    }

    /**
     * A compile-time constant declared in a script's {@code config:} block.
     *
     * @param name the config key
     * @param java the Java expression for the config value
     */
    public record ConfigEntry(@NotNull String name, @NotNull String java) {
    }
}
