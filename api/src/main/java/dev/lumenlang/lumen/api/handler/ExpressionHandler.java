package dev.lumenlang.lumen.api.handler;

import dev.lumenlang.lumen.api.codegen.BindingAccess;
import dev.lumenlang.lumen.api.pattern.PatternRegistrar;
import dev.lumenlang.lumen.api.type.Types;
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
 *     new ExpressionResult("Bukkit.getPlayer(" + ctx.java("name") + ")", Types.PLAYER.id())
 * );
 * }</pre>
 *
 * <p>When the expression evaluates to a primitive or common Java type, use
 * {@link Types} constants:
 * <pre>{@code
 * new ExpressionResult(expr, null, Types.DOUBLE)
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
     * @param javaType  the Java type of the expression result (e.g. {@code Types.INT}, {@code Types.STRING}), or null if unknown
     * @param metadata  compile-time metadata forwarded to the resulting variable reference
     */
    record ExpressionResult(@NotNull String java, @Nullable String refTypeId,
                            @Nullable String javaType,
                            @NotNull Map<String, Object> metadata) {

        /**
         * Creates a typed expression result with no metadata and no explicit Java type.
         *
         * @param java      the Java expression string
         * @param refTypeId the ref type id
         */
        public ExpressionResult(@NotNull String java, @Nullable String refTypeId) {
            this(java, refTypeId, null, Map.of());
        }

        /**
         * Creates a typed expression result with metadata but no explicit Java type.
         *
         * @param java      the Java expression string
         * @param refTypeId the ref type id
         * @param metadata  compile-time metadata
         */
        public ExpressionResult(@NotNull String java, @Nullable String refTypeId,
                                @NotNull Map<String, Object> metadata) {
            this(java, refTypeId, null, metadata);
        }

        /**
         * Creates a typed expression result with an explicit Java type and no metadata.
         *
         * @param java      the Java expression string
         * @param refTypeId the ref type id, or null
         * @param javaType  the Java type name
         */
        public ExpressionResult(@NotNull String java, @Nullable String refTypeId,
                                @Nullable String javaType) {
            this(java, refTypeId, javaType, Map.of());
        }
    }
}
