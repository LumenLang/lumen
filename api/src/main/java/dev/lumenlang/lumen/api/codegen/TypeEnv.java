package dev.lumenlang.lumen.api.codegen;

import dev.lumenlang.lumen.api.codegen.source.SourceMap;
import dev.lumenlang.lumen.api.type.LumenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Read-only view of the compile-time symbol table.
 *
 * <p>Handlers and type bindings use this to look up variables, define new variables,
 * and inspect the current scope. This interface hides internal implementation details of the
 * scope stack.
 *
 * @see HandlerContext
 */
@SuppressWarnings("unused")
public interface TypeEnv {

    /**
     * Source map for the script being compiled.
     */
    @NotNull SourceMap sourceMap();

    /**
     * Looks up a named variable by walking the scope stack from innermost
     * to outermost scope.
     *
     * @param name the variable name to look up
     * @return the variable descriptor, or {@code null} if not found
     */
    @Nullable VarHandle lookupVar(@NotNull String name);

    /**
     * Defines a named variable in the current block scope.
     *
     * @param name the variable name to bind
     * @param type the compile-time type for type checking
     * @param java the Java variable name in generated code
     * @return a reference to the defined variable
     */
    VarHandle defineVar(@NotNull String name, @NotNull LumenType type, @NotNull String java);

    /**
     * Defines a named variable in the current block scope with compile-time metadata.
     *
     * <p>The metadata map is propagated to the resulting {@code VarHandle} so downstream
     * patterns can inspect it for parse-time validation.
     *
     * @param name     the variable name to bind
     * @param type     the compile-time type for type checking
     * @param java     the Java variable name in generated code
     * @param metadata compile-time metadata entries
     * @return a reference to the defined variable
     */
    VarHandle defineVar(@NotNull String name, @NotNull LumenType type, @NotNull String java, @NotNull Map<String, Object> metadata);

    /**
     * Stores an arbitrary key-value pair in the global map.
     *
     * <p>Use this to pass data from a block handler's {@code begin} to its {@code end}.
     *
     * @param key   the key
     * @param value the value to store, or {@code null} to clear
     */
    void put(@NotNull String key, @Nullable Object value);

    /**
     * Retrieves a previously stored global value.
     *
     * @param key the key
     * @param <T> the expected type
     * @return the stored value, or {@code null} if absent
     */
    <T> @Nullable T get(@NotNull String key);

    /**
     * Returns whether the named variable is a stored (persistent) variable.
     *
     * @param name the variable name
     * @return {@code true} if the variable was declared with {@code load}
     */
    boolean isStored(@NotNull String name);

    /**
     * Returns the storage key for a stored variable, or {@code null} if not stored.
     *
     * @param name the variable name
     * @return the storage key, or {@code null}
     */
    @Nullable String getStoredKey(@NotNull String name);

    /**
     * Returns the base key prefix for a stored variable, or {@code null} if not tracked.
     *
     * <p>For scoped stored variables (e.g. {@code load x for player with default 0}), this returns the
     * prefix without the scope suffix, enabling prefix-based deletion of all scoped entries.
     *
     * @param name the variable name
     * @return the base key prefix, or {@code null}
     */
    @Nullable String getStoredBaseKey(@NotNull String name);

    /**
     * Returns the scope variable name for a stored variable, or {@code null} if unscoped.
     *
     * @param name the variable name
     * @return the scope variable name (e.g. {@code "player"}), or {@code null}
     */
    @Nullable String getStoredScopeVar(@NotNull String name);

    /**
     * Returns the simple class name of the persistent variable storage class.
     *
     * <p>Use this to emit storage calls in generated code. The import is added automatically.
     *
     * @return the persistent vars class simple name
     */
    @NotNull String persistClassName();

    /**
     * Returns the fully-qualified storage class name for the given variable.
     *
     * <p>For persistent (stored) variables, this returns the same as {@link #persistClassName()}.
     * For runtime (non-stored) globals, this returns the in-memory storage class name.
     *
     * @param name the variable name
     * @return the appropriate storage class name
     */
    default @NotNull String storedClassName(@NotNull String name) {
        return persistClassName();
    }

    /**
     * Returns whether the named variable was declared as a script-wide global.
     *
     * @param name the variable name
     * @return {@code true} if declared with {@code global}
     */
    boolean isGlobal(@NotNull String name);

    /**
     * Returns the declaration info for a global variable, or {@code null} if not declared.
     *
     * @param name the variable name
     * @return the global variable info, or {@code null}
     */
    @Nullable GlobalInfo getGlobalInfo(@NotNull String name);

    /**
     * Returns an unmodifiable list of all registered global variables as handles.
     *
     * <p>Each handle exposes the source name, declared type, metadata, and the
     * {@link GlobalInfo} descriptor through {@link VarHandle#globalInfo()}.
     *
     * @return the global variable handles
     */
    @NotNull List<? extends VarHandle> allGlobals();

    /**
     * Marks a variable as stored (persistent) with scope information.
     *
     * <p>Stored variables are auto-saved when modified by math operations.
     *
     * @param name     the variable name
     * @param keyExpr  the full storage key expression
     * @param baseKey  the base key prefix without the scope part
     * @param scopeVar the scope variable name, or {@code null} if unscoped
     */
    void markStored(@NotNull String name, @NotNull String keyExpr, @NotNull String baseKey, @Nullable String scopeVar);

    /**
     * Marks a variable as a runtime (non-persistent) global.
     *
     * <p>Runtime globals use in-memory storage instead of persistent storage.
     * They survive across block invocations but are lost on restart.
     *
     * @param name the variable name
     */
    void markRuntimeGlobal(@NotNull String name);

    /**
     * Marks a global variable as being backed by a class-level field rather than
     * a block-scoped local variable.
     *
     * <p>Field-backed globals can be safely mutated inside lambdas (schedule blocks,
     * event handlers) because they are accessed via {@code this.fieldName} rather
     * than a captured local.
     *
     * @param name the variable name
     */
    void markGlobalField(@NotNull String name);

    /**
     * Returns whether the named variable is backed by a class-level field.
     *
     * @param name the variable name
     * @return {@code true} if the variable uses a class field
     */
    boolean isGlobalField(@NotNull String name);

    /**
     * Returns {@code true} if the named variable would be captured from an outer scope
     * by a lambda block (e.g. a schedule block).
     *
     * <p>A variable is considered captured if it was defined in a scope above the nearest
     * {@code __lambda_block} boundary. Variables defined inside the lambda body are
     * lambda-local and are not captured.
     *
     * @param name the variable name to check
     * @return {@code true} if the variable is captured by a lambda and therefore must be
     * effectively final (unless it is a class-level field)
     */
    boolean isVarCapturedByLambda(@NotNull String name);

    /**
     * Registers a config entry from the script's config block.
     *
     * @param name      the config key
     * @param javaValue the Java expression for the config value
     */
    void registerConfig(@NotNull String name, @NotNull String javaValue);

    /**
     * Defines a variable at the root (class) scope, making it visible from all block contexts.
     *
     * @param name the variable name
     * @param type the compile-time type for type checking
     * @param java the Java variable name in generated code
     * @return a reference to the defined variable
     */
    VarHandle defineRootVar(@NotNull String name, @NotNull LumenType type, @NotNull String java);

    /**
     * Current block context. Always present during normal handler emit.
     *
     * @throws IllegalStateException when called outside any active block, which does not happen during normal compilation
     */
    @NotNull BlockContext block();

    /**
     * Marks a variable as definitively non-null at this point in the code.
     * Used by smart-cast narrowing after null checks (e.g. "if x is set:").
     *
     * @param javaName the Java variable name
     */
    void markNonNull(@NotNull String javaName);

    /**
     * Restores a variable's null state to its previous value after leaving a narrowing scope.
     *
     * @param javaName the Java variable name
     */
    void clearNonNull(@NotNull String javaName);

    /**
     * Publishes a narrowing fact derived from the condition currently being evaluated.
     *
     * @param fact the narrowing fact produced by the condition
     */
    void pushNarrowing(@NotNull NarrowingFact fact);

    /**
     * Drains and returns all pending narrowing facts published since the last call.
     * Block handlers call this at the start of their body to collect facts that should
     * apply to the body, and again at the end to decide what the sibling {@code else}
     * branch should invert.
     *
     * @return an immutable snapshot of the pending facts, possibly empty
     */
    @NotNull List<NarrowingFact> consumeNarrowings();

    /**
     * Applies the given facts to the current scope so that later uses of the referenced
     * variables see the narrowed state. Callers must pair this with
     * {@link #clearNarrowings} at the end of the scope.
     *
     * @param facts the facts to apply
     */
    void applyNarrowings(@NotNull List<NarrowingFact> facts);

    /**
     * Reverts narrowing state previously applied via {@link #applyNarrowings}.
     *
     * @param facts the facts to revert
     */
    void clearNarrowings(@NotNull List<NarrowingFact> facts);

    /**
     * A compile-time descriptor for a named variable that is in scope.
     *
     * @see #lookupVar(String)
     */
    interface VarHandle {

        /**
         * Returns the script-level name of this variable as it appears in source.
         *
         * @return the source name
         */
        @NotNull String name();

        /**
         * Returns the compile-time type of this variable.
         *
         * @return the compile-time type
         */
        @NotNull LumenType type();

        /**
         * Returns the Java variable name that this variable maps to in generated code.
         *
         * <p>Throws for scoped globals, which have no single standalone Java expression.
         * Callers handling scoped globals must consult {@link #globalInfo()} and build
         * storage accesses with an explicit scope.
         *
         * @return the Java variable expression
         */
        @NotNull String java();

        /**
         * Returns the metadata value for the given key, or {@code null} if absent.
         *
         * @param key the metadata key
         * @return the value, or {@code null}
         */
        @Nullable Object meta(@NotNull String key);

        /**
         * Returns {@code true} if metadata contains the given key.
         *
         * @param key the metadata key
         * @return {@code true} if present
         */
        boolean hasMeta(@NotNull String key);

        /**
         * Returns an unmodifiable view of all metadata entries.
         *
         * @return the metadata map
         */
        @NotNull Map<String, Object> metadata();

        /**
         * Returns the global declaration info if this variable is a script-wide global,
         * or {@code null} for locals and root variables.
         *
         * @return the global declaration info, or {@code null}
         */
        default @Nullable GlobalInfo globalInfo() {
            return null;
        }
    }

    /**
     * Compile-time descriptor for a script-wide global variable.
     */
    interface GlobalInfo {

        /**
         * Returns the Java expression for the default value.
         *
         * @return the default value expression
         */
        @NotNull String defaultJava();

        /**
         * Returns the script class name at the time of declaration.
         *
         * @return the class name
         */
        @NotNull String className();

        /**
         * Returns whether this global is scoped per entity rather than server-wide.
         *
         * @return {@code true} if scoped
         */
        boolean scoped();

        /**
         * Returns whether the variable is persisted to disk.
         *
         * @return {@code true} if persistent
         */
        boolean stored();

        /**
         * Returns the scope type for scoped globals (e.g. {@code player}, {@code entity}).
         *
         * @return the scope type, or {@code null} if the global is not scoped
         */
        default @Nullable LumenType scopeType() {
            return null;
        }
    }
}
