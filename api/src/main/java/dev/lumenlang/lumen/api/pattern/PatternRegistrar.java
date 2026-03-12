package dev.lumenlang.lumen.api.pattern;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.handler.BlockHandler;
import dev.lumenlang.lumen.api.handler.ConditionHandler;
import dev.lumenlang.lumen.api.handler.ExpressionHandler;
import dev.lumenlang.lumen.api.handler.LoopHandler;
import dev.lumenlang.lumen.api.handler.StatementHandler;
import dev.lumenlang.lumen.api.pattern.builder.BlockBuilder;
import dev.lumenlang.lumen.api.pattern.builder.ConditionBuilder;
import dev.lumenlang.lumen.api.pattern.builder.ExpressionBuilder;
import dev.lumenlang.lumen.api.pattern.builder.LoopBuilder;
import dev.lumenlang.lumen.api.pattern.builder.StatementBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

/**
 * API handle for registering statement, block, condition, and expression patterns.
 *
 * <p>Patterns can be registered in two ways:
 * <ol>
 *   <li><b>Simple (legacy):</b> directly passing a pattern string and handler</li>
 *   <li><b>Builder (recommended):</b> using a consumer that configures a builder with
 *       documentation metadata such as description, examples, category, and version</li>
 * </ol>
 *
 * <h2>Builder Example</h2>
 * <pre>{@code
 * api.patterns().condition(b -> b
 *     .pattern("%p:PLAYER% is swimming")
 *     .description("Checks whether a player is currently swimming.")
 *     .example("if player is swimming:")
 *     .since("1.0.0")
 *     .category(Categories.PLAYER)
 *     .handler((match, env, ctx) -> match.ref("p").java() + ".isSwimming()")
 * );
 * }</pre>
 *
 * <h2>Simple Example</h2>
 * <pre>{@code
 * api.patterns().statement("explode %who:PLAYER%",
 *         (line, ctx, out) -> out.line(ctx.java("who") + ".getWorld().createExplosion(" +
 *                 ctx.java("who") + ".getLocation(), 4F);"));
 * }</pre>
 *
 * @see LumenAPI#patterns()
 */
public interface PatternRegistrar {

    /**
     * Registers a statement pattern with its handler.
     *
     * @param pattern the pattern string (e.g. {@code "explode %who:PLAYER%"})
     * @param handler the handler that generates Java code for this statement
     */
    void statement(@NotNull String pattern, @NotNull StatementHandler handler);

    /**
     * Registers multiple pattern strings that all map to the same statement
     * handler.
     *
     * @param patterns the list of pattern strings
     * @param handler  the handler that generates Java code for these statements
     */
    void statement(@NotNull List<String> patterns, @NotNull StatementHandler handler);

    /**
     * Registers a statement pattern using a builder for documentation metadata.
     *
     * @param builderConsumer consumer that configures the builder
     */
    void statement(@NotNull Consumer<StatementBuilder> builderConsumer);

    /**
     * Registers a block pattern with its handler.
     *
     * @param pattern the pattern string (e.g. {@code "repeat %n:INT% times"})
     * @param handler the handler that generates Java code for this block
     */
    void block(@NotNull String pattern, @NotNull BlockHandler handler);

    /**
     * Registers multiple block pattern strings that all map to the same handler.
     *
     * @param patterns the list of pattern strings
     * @param handler  the handler that generates Java code for this block
     */
    void block(@NotNull List<String> patterns, @NotNull BlockHandler handler);

    /**
     * Registers a block pattern using a builder for documentation metadata.
     *
     * @param builderConsumer consumer that configures the builder
     */
    void block(@NotNull Consumer<BlockBuilder> builderConsumer);

    /**
     * Registers a condition pattern with its handler.
     *
     * @param pattern the pattern string (e.g. {@code "%p:PLAYER% is swimming"})
     * @param handler the handler that generates a Java boolean expression
     */
    void condition(@NotNull String pattern, @NotNull ConditionHandler handler);

    /**
     * Registers multiple condition pattern strings that all map to the same
     * handler.
     *
     * @param patterns the list of pattern strings
     * @param handler  the handler that generates a Java boolean expression
     */
    void condition(@NotNull List<String> patterns, @NotNull ConditionHandler handler);

    /**
     * Registers a condition pattern using a builder for documentation metadata.
     *
     * @param builderConsumer consumer that configures the builder
     */
    void condition(@NotNull Consumer<ConditionBuilder> builderConsumer);

    /**
     * Registers an expression pattern with its handler.
     *
     * <p>
     * Expression patterns are used in {@code var x = <pattern>} statement where
     * the
     * handler returns a Java expression that evaluates to the variable's value.
     *
     * @param pattern the pattern string (e.g. {@code "get player %name:STRING%"})
     * @param handler the handler that returns a Java expression result
     */
    void expression(@NotNull String pattern, @NotNull ExpressionHandler handler);

    /**
     * Registers multiple expression pattern strings that all map to the same
     * handler.
     *
     * @param patterns the list of pattern strings
     * @param handler  the handler that returns a Java expression result
     */
    void expression(@NotNull List<String> patterns, @NotNull ExpressionHandler handler);

    /**
     * Registers an expression pattern using a builder for documentation metadata.
     *
     * @param builderConsumer consumer that configures the builder
     */
    void expression(@NotNull Consumer<ExpressionBuilder> builderConsumer);

    /**
     * Registers a loop source pattern with its handler.
     *
     * <p>Loop sources define what collection a {@code loop ... in <source>:} block iterates over.
     *
     * @param pattern the pattern string (e.g. {@code "all players"})
     * @param handler the handler that returns an iterable expression and element type
     */
    void loop(@NotNull String pattern, @NotNull LoopHandler handler);

    /**
     * Registers multiple loop source pattern strings that all map to the same handler.
     *
     * @param patterns the list of pattern strings
     * @param handler  the handler that returns an iterable expression and element type
     */
    void loop(@NotNull List<String> patterns, @NotNull LoopHandler handler);

    /**
     * Registers a loop source pattern using a builder for documentation metadata.
     *
     * @param builderConsumer consumer that configures the builder
     */
    void loop(@NotNull Consumer<LoopBuilder> builderConsumer);
}
