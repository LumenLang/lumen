package dev.lumenlang.lumen.api.handler;

import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.type.LumenType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Handles a matched expression pattern by returning a Java expression string that
 * can be assigned to a variable.
 *
 * <p>Unlike a {@link StatementHandler} which emits lines of Java code, an
 * {@code ExpressionHandler} produces a single Java expression that evaluates to
 * a value. This is used for "returnable" patterns in {@code set x to <pattern>} statement.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * api.patterns().expression("get player %name:STRING%", ctx ->
 *     new ExpressionResult("Bukkit.getPlayer(" + ctx.java("name") + ")", MinecraftTypes.PLAYER)
 * );
 * }</pre>
 *
 * <p>Scripts can then write:
 * <pre>{@code
 * set target to get player "Notch"
 * }</pre>
 */
@FunctionalInterface
public interface ExpressionHandler {

    /**
     * Generates a Java expression for the matched pattern.
     *
     * @param ctx the handler context providing bound parameters and environment
     * @return the expression result containing the Java expression and its type
     */
    @NotNull ExpressionResult handle(@NotNull HandlerContext ctx);

    /**
     * The result of an expression handler.
     *
     * @param java     the Java expression string
     * @param type     the compile-time type of the resulting expression
     * @param metadata compile-time metadata forwarded to the resulting variable reference
     */
    record ExpressionResult(@NotNull String java, @NotNull LumenType type, @NotNull Map<String, Object> metadata) {

        /**
         * Creates a typed expression result with no metadata.
         *
         * @param java the Java expression string
         * @param type the compile-time type of the expression
         */
        public ExpressionResult(@NotNull String java, @NotNull LumenType type) {
            this(java, type, Map.of());
        }
    }
}
