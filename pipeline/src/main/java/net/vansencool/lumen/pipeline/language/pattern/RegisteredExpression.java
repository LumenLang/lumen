package net.vansencool.lumen.pipeline.language.pattern;

import net.vansencool.lumen.api.handler.ExpressionHandler;
import net.vansencool.lumen.api.pattern.PatternMeta;
import org.jetbrains.annotations.NotNull;

/**
 * Associates a compiled {@link Pattern} with the {@link ExpressionHandler} that should be invoked
 * when the pattern is matched in a {@code var x = <pattern>} context.
 *
 * @param pattern the compiled pattern to match against expression token lists
 * @param handler the handler invoked on a successful match
 * @param meta    documentation metadata for this pattern
 * @see PatternRegistry#expression(String, ExpressionHandler)
 */
public record RegisteredExpression(@NotNull Pattern pattern, @NotNull ExpressionHandler handler,
                                   @NotNull PatternMeta meta) {

    public RegisteredExpression(@NotNull Pattern pattern, @NotNull ExpressionHandler handler) {
        this(pattern, handler, PatternMeta.EMPTY);
    }
}
