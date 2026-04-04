package dev.lumenlang.lumen.api.codegen;

import dev.lumenlang.lumen.api.type.RefTypeHandle;
import dev.lumenlang.lumen.api.type.TypeHandle;
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
 * @see BindingAccess
 */
@SuppressWarnings("unused")
public interface EnvironmentAccess {

    /**
     * Looks up a named variable by walking the scope stack from innermost
     * to outermost scope.
     *
     * @param name the variable name to look up
     * @return the variable descriptor, or {@code null} if not found
     */
    @Nullable VarHandle lookupVar(@NotNull String name);

    /**
     * Returns the first variable in scope whose ref type matches the given type.
     *
     * <p>Walks the scope stack from innermost to outermost scope, examining all variables
     * in each frame.
     *
     * @param type the ref type to match against
     * @return the first matching variable, or {@code null} if none found
     */
    @Nullable VarHandle lookupVarByType(@NotNull RefTypeHandle type);

    /**
     * Defines a named variable in the current block scope.
     *
     * @param name    the variable name to bind
     * @param refType the ref type for type checking, or {@code null} for plain variables
     * @param java    the Java variable name in generated code
     * @return a reference to the defined variable
     */
    VarHandle defineVar(@NotNull String name, @Nullable RefTypeHandle refType, @NotNull String java);

    /**
     * Defines a named variable in the current block scope with compile-time metadata.
     *
     * <p>The metadata map is propagated to the resulting {@code VarHandle} so downstream
     * patterns can inspect it for parse-time validation.
     *
     * @param name     the variable name to bind
     * @param refType  the ref type for type checking, or {@code null} for plain variables
     * @param java     the Java variable name in generated code
     * @param metadata compile-time metadata entries
     * @return a reference to the defined variable
     */
    VarHandle defineVar(@NotNull String name, @Nullable RefTypeHandle refType,
                        @NotNull String java, @NotNull Map<String, Object> metadata);

    /**
     * Stores an arbitrary key-value pair in the global map.
     *
     * <p>Use this to pass data from a block handler's {@code begin} to its {@code end}.
     *
     * @param key   the key
     * @param value the value to store
     */
    void put(@NotNull String key, @NotNull Object value);

    /**
     * Retrieves a previously stored global value.
     *
     * @param key the key
     * @param <T> the expected type
     * @return the stored value, or {@code null} if absent
     */
    <T> @Nullable T get(@NotNull String key);

    /**
     * Expands placeholders expressions embedded in a raw string value.
     *
     * <p>Placeholders use the syntax {@code {variable_property}} or {@code {variable}}.
     * If no placeholders are present, the string is returned as a quoted Java literal.
     *
     * @param raw the raw string content (without surrounding quotes)
     * @return a Java expression string (may use concatenation for embedded placeholders)
     */
    @NotNull String expandPlaceholders(@NotNull String raw);

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
     * Registers a script-wide global variable declaration.
     *
     * <p>Global variables are loaded automatically at the start of every block body and
     * are auto-saved on modification.
     *
     * @param info the global variable declaration information
     */
    void registerGlobal(@NotNull GlobalInfo info);

    /**
     * Returns an unmodifiable list of all registered global variable declarations.
     *
     * @return the global variable declarations
     */
    @NotNull List<? extends GlobalInfo> allGlobals();

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
    void markStored(@NotNull String name, @NotNull String keyExpr,
                    @NotNull String baseKey, @Nullable String scopeVar);

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
     * @param name    the variable name
     * @param refType the ref type for type checking, or {@code null} for plain variables
     * @param java    the Java variable name in generated code
     * @return a reference to the defined variable
     */
    VarHandle defineRootVar(@NotNull String name, @Nullable RefTypeHandle refType, @NotNull String java);

    /**
     * A compile-time descriptor for a named variable that is in scope.
     *
     * @see #lookupVar(String)
     */
    interface VarHandle {

        /**
         * Returns the ref type of this variable, or {@code null} for plain variables.
         *
         * @return the ref type, or {@code null}
         */
        @Nullable RefTypeHandle type();

        /**
         * Returns the full compile-time type of this variable, or {@code null} if unknown.
         *
         * <p>This provides richer type information than {@link #type()}, covering
         * primitives and generic collections in addition to object reference types.
         *
         * @return the type handle, or {@code null}
         */
        default @Nullable TypeHandle typeHandle() {
            return null;
        }

        /**
         * Returns the Java variable name that this variable maps to in generated code.
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
    }

    /**
     * Compile-time descriptor for a script-wide global variable declared with {@code global var}.
     */
    interface GlobalInfo {

        /**
         * Returns the variable name.
         *
         * @return the name
         */
        @NotNull String name();

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
         * Returns the ref type ID used as the scope qualifier for this global, or {@code null} for server-wide globals.
         *
         * @return the ref type identifier, or {@code null}
         */
        @Nullable String refTypeName();

        /**
         * Returns whether the variable is persisted to disk.
         *
         * @return {@code true} if persistent
         */
        boolean stored();

        /**
         * Returns the ref type ID inferred from the default expression, or {@code null}.
         *
         * @return the expression ref type ID, or {@code null}
         */
        default @Nullable String exprRefTypeId() {
            return null;
        }

        /**
         * Returns compile-time metadata from the default expression, or {@code null}.
         *
         * @return the expression metadata, or {@code null}
         */
        default @Nullable Map<String, Object> exprMetadata() {
            return null;
        }
    }
}
