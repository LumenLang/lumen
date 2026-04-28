package dev.lumenlang.lumen.pipeline.language.pattern.registered;

import dev.lumenlang.lumen.api.handler.ExpressionHandler;
import dev.lumenlang.lumen.api.pattern.PatternMeta;
import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import org.jetbrains.annotations.NotNull;

/**
 * Associates a compiled {@link Pattern} with the {@link ExpressionHandler} that should be invoked
 * when the pattern is matched in a {@code set x to <pattern>} context.
 *
 * @param pattern the compiled pattern to match against expression token lists
 * @param handler the handler invoked on a successful match
 * @param meta    documentation metadata for this pattern
 */
public record RegisteredExpression(@NotNull Pattern pattern, @NotNull ExpressionHandler handler, @NotNull PatternMeta meta) {

    public RegisteredExpression(@NotNull Pattern pattern, @NotNull ExpressionHandler handler) {
        this(pattern, handler, PatternMeta.EMPTY);
    }
}
