package dev.lumenlang.lumen.pipeline.language.pattern;

import dev.lumenlang.lumen.api.handler.ExpressionHandler;
import dev.lumenlang.lumen.api.pattern.PatternMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Associates a compiled {@link Pattern} with the {@link ExpressionHandler} that should be invoked
 * when the pattern is matched in a {@code var x = <pattern>} context.
 *
 * <p>The optional {@code returnRefTypeId} declares the ref type this expression
 * statically produces (e.g. "PLAYER", "LOCATION"). The optional {@code returnJavaType}
 * declares the Java type for primitive or string results (e.g. "int", "String").
 * Tooling can use these to resolve the type of a variable assigned from this
 * expression without executing the handler. Expressions whose return type depends
 * on runtime input may leave both {@code null}.
 *
 * @param pattern          the compiled pattern to match against expression token lists
 * @param handler          the handler invoked on a successful match
 * @param meta             documentation metadata for this pattern
 * @param returnRefTypeId  the ref type id this expression always returns, or {@code null} if dynamic
 * @param returnJavaType   the Java type this expression always returns (e.g. "int", "String"), or {@code null}
 * @see PatternRegistry#expression(String, ExpressionHandler)
 */
public record RegisteredExpression(@NotNull Pattern pattern, @NotNull ExpressionHandler handler,
                                   @NotNull PatternMeta meta,
                                   @Nullable String returnRefTypeId,
                                   @Nullable String returnJavaType) {

    public RegisteredExpression(@NotNull Pattern pattern, @NotNull ExpressionHandler handler) {
        this(pattern, handler, PatternMeta.EMPTY, null, null);
    }

    public RegisteredExpression(@NotNull Pattern pattern, @NotNull ExpressionHandler handler,
                                @NotNull PatternMeta meta) {
        this(pattern, handler, meta, null, null);
    }

    public RegisteredExpression(@NotNull Pattern pattern, @NotNull ExpressionHandler handler,
                                @NotNull PatternMeta meta, @Nullable String returnRefTypeId) {
        this(pattern, handler, meta, returnRefTypeId, null);
    }
}
