package net.vansencool.lumen.api.type;

import net.vansencool.lumen.api.codegen.CodegenAccess;
import net.vansencool.lumen.api.codegen.EnvironmentAccess;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * API-level type binding that addons implement to teach Lumen how to parse and code-generate
 * a new placeholders type.
 *
 * <p>Register instances with {@link TypeRegistrar#register(AddonTypeBinding)}.
 * If the binding works with a custom reference type, register that type first via
 * {@link RefTypeRegistrar} so it participates in default variable resolution.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * api.types().register(new AddonTypeBinding() {
 *     public @NotNull String id() { return "ENTITY"; }
 *
 *     public int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
 *         return 1;
 *     }
 *
 *     public Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
 *         return env.lookupVar(tokens.get(0));
 *     }
 *
 *     public @NotNull String toJava(Object value, @NotNull CodegenAccess ctx,
 *                                    @NotNull EnvironmentAccess env) {
 *         return ((EnvironmentAccess.VarHandle) value).java();
 *     }
 * });
 * }</pre>
 *
 * @see TypeRegistrar
 * @see RefTypeRegistrar
 */
public interface AddonTypeBinding {

    /**
     * Returns the unique identifier for this type binding.
     *
     * @return the type binding identifier (e.g. {@code "ENTITY"})
     */
    @NotNull String id();

    /**
     * Parses the given token texts into a runtime value.
     *
     * @param tokens the token texts to parse
     * @param env    the current type environment
     * @return the parsed value
     */
    Object parse(@NotNull List<String> tokens, @NotNull EnvironmentAccess env);

    /**
     * Converts the parsed value into a Java source expression.
     *
     * @param value the value previously returned by {@link #parse}
     * @param ctx   the code generation context
     * @param env   the current type environment
     * @return valid Java source code representing this value
     */
    @NotNull String toJava(Object value, @NotNull CodegenAccess ctx, @NotNull EnvironmentAccess env);

    /**
     * Determines how many tokens this type binding should consume.
     *
     * @param tokens the remaining token texts available
     * @param env    the current type environment
     * @return number of tokens to consume, or {@code -1} for all remaining
     */
    default int consumeCount(@NotNull List<String> tokens, @NotNull EnvironmentAccess env) {
        return -1;
    }

    /**
     * Returns the documentation metadata for this type binding.
     *
     * <p>Override this method to provide a description, Java type information,
     * usage examples, and version tracking for documentation generation.
     *
     * @return the metadata for this type binding, never null
     */
    default @NotNull TypeBindingMeta meta() {
        return TypeBindingMeta.EMPTY;
    }
}
