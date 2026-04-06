package dev.lumenlang.lumen.api.handler;

import dev.lumenlang.lumen.api.codegen.CodegenAccess;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.pattern.PatternRegistrar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles a matched condition pattern by returning a Java boolean expression as
 * a string.
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * api.patterns().condition("%p:PLAYER% is swimming", (match, env, ctx) -> match.ref("p").java() + ".isSwimming()");
 * }</pre>
 *
 * @see PatternRegistrar#condition(String, ConditionHandler)
 */
@FunctionalInterface
public interface ConditionHandler {

    /**
     * Generates a Java boolean expression for the matched condition.
     *
     * @param match the match result providing convenience accessors
     * @param env   the current type environment
     * @param ctx   the code generation context
     * @return a valid Java boolean expression string
     */
    @NotNull
    String handle(@NotNull ConditionMatch match,
                  @NotNull EnvironmentAccess env,
                  @NotNull CodegenAccess ctx);

    /**
     * Provides convenient access to matched condition parameters.
     */
    interface ConditionMatch {

        /**
         * Returns the parsed value for the named parameter, cast to the expected type.
         *
         * @param name the parameter name
         * @param <T>  the expected type
         * @return the parsed value
         */
        <T> @NotNull T value(@NotNull String name);

        /**
         * Returns the parsed value as a variable handle.
         *
         * @param name the parameter name
         * @return the variable handle
         */
        @NotNull EnvironmentAccess.VarHandle ref(@NotNull String name);

        /**
         * Converts the bound value for the named parameter into a Java source
         * expression.
         *
         * @param name the parameter name
         * @param ctx  the codegen access
         * @param env  the environment access
         * @return the generated Java source expression
         */
        @NotNull
        String java(@NotNull String name, @NotNull CodegenAccess ctx, @NotNull EnvironmentAccess env);

        /**
         * Returns the matched alternative text for the Nth required choice group.
         *
         * @param index the zero-based index of the required choice group
         * @return the matched alternative text, or null if out of range
         */
        @Nullable
        String choice(int index);

        /**
         * Returns the parsed value at the given positional index, cast to the expected type.
         *
         * <p>Index order corresponds to the order in which placeholders appear in
         * the pattern (left to right).
         *
         * @param index the zero-based index
         * @param <T>   the expected type
         * @return the parsed value
         * @throws IndexOutOfBoundsException if the index is out of range
         */
        <T> @NotNull T value(int index);

        /**
         * Returns the parsed value at the given positional index as a variable handle.
         *
         * @param index the zero-based index
         * @return the variable handle
         * @throws IndexOutOfBoundsException if the index is out of range
         */
        @NotNull EnvironmentAccess.VarHandle ref(int index);

        /**
         * Converts the bound value at the given positional index into a Java source expression.
         *
         * @param index the zero-based index
         * @param ctx   the codegen access
         * @param env   the environment access
         * @return the generated Java source expression
         * @throws IndexOutOfBoundsException if the index is out of range
         */
        @NotNull
        String java(int index, @NotNull CodegenAccess ctx, @NotNull EnvironmentAccess env);

        /**
         * Returns the number of bound parameters in this match.
         *
         * @return the parameter count
         */
        int size();
    }
}
