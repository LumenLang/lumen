package net.vansencool.lumen.api.handler;

import net.vansencool.lumen.api.codegen.BindingAccess;
import net.vansencool.lumen.api.pattern.PatternRegistrar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Handles a matched expression pattern by returning a Java expression string that
 * can be assigned to a variable.
 *
 * <p>Unlike a {@link StatementHandler} which emits lines of Java code, an
 * {@code ExpressionHandler} produces a single Java expression that evaluates to
 * a value. This is used for "returnable" patterns in {@code var x = <pattern>} statement.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * api.patterns().expression("get player %name:STRING%", ctx ->
 *     new ExpressionResult("org.bukkit.Bukkit.getPlayer(" + ctx.java("name") + ")", "PLAYER")
 * );
 * }</pre>
 *
 * <p>Scripts can then write:
 * <pre>{@code
 * var target = get player "Notch"
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
     * @return the expression result containing the Java expression and optional ref type id
     */
    @NotNull ExpressionResult handle(@NotNull BindingAccess ctx);

    /**
     * The result of an expression handler.
     *
     * @param java      the Java expression string
     * @param refTypeId the ref type id for the resulting variable (e.g. "PLAYER"), or null if untyped
     * @param metadata  compile-time metadata forwarded to the resulting variable reference
     */
    record ExpressionResult(@NotNull String java, @Nullable String refTypeId,
                            @NotNull Map<String, Object> metadata) {

        /**
         * Creates a typed expression result with no metadata.
         *
         * @param java      the Java expression string
         * @param refTypeId the ref type id
         */
        public ExpressionResult(@NotNull String java, @Nullable String refTypeId) {
            this(java, refTypeId, Map.of());
        }
    }
}
