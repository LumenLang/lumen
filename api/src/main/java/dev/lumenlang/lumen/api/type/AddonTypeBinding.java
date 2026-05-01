package dev.lumenlang.lumen.api.type;

import dev.lumenlang.lumen.api.codegen.CodegenContext;
import dev.lumenlang.lumen.api.codegen.TypeEnv;
import dev.lumenlang.lumen.api.exceptions.ParseFailureException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * API-level type binding that addons implement to teach Lumen how to parse and code-generate
 * a new placeholders type.
 *
 * <p>Register instances with {@link TypeRegistrar#register(AddonTypeBinding)}.
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
     * <p>
     * To <b>reject</b> a match, throw {@link ParseFailureException}. The pattern
     * matcher catches it and treats the binding as a non-match.
     *
     * @param tokens the token texts to parse
     * @param env    the current type environment
     * @return the parsed value
     */
    Object parse(@NotNull List<String> tokens, @NotNull TypeEnv env);

    /**
     * Converts the parsed value into a Java source expression.
     *
     * @param value the value previously returned by {@link #parse}
     * @param ctx   the code generation context
     * @param env   the current type environment
     * @return valid Java source code representing this value
     */
    @NotNull String toJava(Object value, @NotNull CodegenContext ctx, @NotNull TypeEnv env);

    /**
     * Determines how many tokens this type binding should consume.
     *
     * <p>
     * Return values:
     * <ul>
     * <li><b>-1</b>: Consume all remaining tokens until the next literal or
     * end of stream. Used by types like EXPR and COND.</li>
     * <li><b>0</b>: Consume no tokens, but still call {@link #parse} with an
     * empty list. This signals that the binding produces a value without
     * consuming input (e.g. a default or implicit value).</li>
     * <li><b>1+</b>: Consume exactly this many tokens.</li>
     * </ul>
     *
     * <p>
     * To <b>reject</b> a match, throw {@link ParseFailureException}. The pattern
     * matcher catches it and treats the binding as a non-match.
     *
     * <p>
     * The default implementation throws {@link ParseFailureException} when the
     * token list is empty and returns {@code 1} otherwise.
     *
     * @param tokens the remaining token texts available
     * @param env    the current type environment
     * @return number of tokens to consume, or {@code -1} for all remaining
     * @throws ParseFailureException if this binding cannot match the given tokens
     */
    default int consumeCount(@NotNull List<String> tokens, @NotNull TypeEnv env) {
        if (tokens.isEmpty())
            throw new ParseFailureException("expected a value here");
        return 1;
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
