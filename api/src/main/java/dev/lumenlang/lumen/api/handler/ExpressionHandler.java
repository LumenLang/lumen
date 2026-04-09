package dev.lumenlang.lumen.api.handler;

import dev.lumenlang.lumen.api.codegen.BindingAccess;
import dev.lumenlang.lumen.api.pattern.PatternRegistrar;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.api.type.PrimitiveType;
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
 * <p>When the expression evaluates to a primitive or common Java type, use
 * {@link PrimitiveType} constants:
 * <pre>{@code
 * new ExpressionResult(expr, PrimitiveType.DOUBLE)
 * }</pre>
 *
 * <p>Scripts can then write:
 * <pre>{@code
 * set target to get player "Notch"
 * }</pre>
 *
 * @see PatternRegistrar#expression(String, ExpressionHandler)
 */
@FunctionalInterface
public interface ExpressionHandler {

    /**
     * Generates a Java expression for the matched pattern.
     *
     * @param ctx the bound parameters from the pattern match
     * @return the expression result containing the Java expression and its type
     */
    @NotNull ExpressionResult handle(@NotNull BindingAccess ctx);

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
