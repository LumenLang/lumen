package dev.lumenlang.lumen.api.pattern;

import dev.lumenlang.lumen.api.handler.BlockHandler;
import dev.lumenlang.lumen.api.handler.ConditionHandler;
import dev.lumenlang.lumen.api.handler.ExpressionHandler;
import dev.lumenlang.lumen.api.handler.LoopHandler;
import dev.lumenlang.lumen.api.handler.StatementHandler;
import dev.lumenlang.lumen.api.inject.body.InjectableBody;
import dev.lumenlang.lumen.api.inject.body.InjectableCondition;
import dev.lumenlang.lumen.api.inject.body.InjectableExpression;
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
 *   <li><b>Simple:</b> directly passing a pattern string and handler</li>
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
     * Expression patterns are used in {@code set x to <pattern>} statement where
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

    /**
     * Registers a statement pattern with an injectable body.
     *
     * <p>The body's bytecode is extracted at registration time and injected into
     * the compiled script class after compilation.
     *
     * <pre>{@code
     * api.patterns().injectable("greet %who:PLAYER%", () -> {
     *     Player player = Fakes.fake("who");
     *     player.sendMessage("Hello " + player.getName() + "!");
     * });
     * }</pre>
     *
     * @param pattern the pattern string
     * @param body the injectable body whose bytecode will be extracted and injected
     */
    void injectable(@NotNull String pattern, @NotNull InjectableBody body);

    /**
     * Registers multiple pattern strings that all map to the same injectable body.
     *
     * @param patterns the list of pattern strings
     * @param body the injectable body whose bytecode will be extracted and injected
     */
    void injectable(@NotNull List<String> patterns, @NotNull InjectableBody body);

    /**
     * Registers an expression pattern with an injectable expression.
     *
     * <p>The expression's bytecode is extracted at registration time and injected into
     * the compiled script class after compilation.
     *
     * <pre>{@code
     * api.patterns().injectableExpression("location of %who:PLAYER%", () -> {
     *     Player player = Fakes.fake("who");
     *     return player.getLocation();
     * });
     * }</pre>
     *
     * @param pattern the pattern string
     * @param expression the injectable expression
     */
    void injectableExpression(@NotNull String pattern, @NotNull InjectableExpression expression);

    /**
     * Registers multiple pattern strings that all map to the same injectable expression.
     *
     * @param patterns the list of pattern strings
     * @param expression the injectable expression
     */
    void injectableExpression(@NotNull List<String> patterns, @NotNull InjectableExpression expression);

    /**
     * Registers a condition pattern with an injectable condition.
     *
     * <p>The condition's bytecode is extracted at registration time and injected into
     * the compiled script class after compilation.
     *
     * <pre>{@code
     * api.patterns().injectableCondition("%p:PLAYER% is swimming", () -> {
     *     Player player = Fakes.fake("p");
     *     return player.isSwimming();
     * });
     * }</pre>
     *
     * @param pattern the pattern string
     * @param condition the injectable condition
     */
    void injectableCondition(@NotNull String pattern, @NotNull InjectableCondition condition);

    /**
     * Registers multiple pattern strings that all map to the same injectable condition.
     *
     * @param patterns the list of pattern strings
     * @param condition the injectable condition
     */
    void injectableCondition(@NotNull List<String> patterns, @NotNull InjectableCondition condition);
}
