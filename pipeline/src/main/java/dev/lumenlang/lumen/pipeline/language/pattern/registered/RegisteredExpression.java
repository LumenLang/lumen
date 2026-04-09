package dev.lumenlang.lumen.pipeline.language.pattern.registered;

import dev.lumenlang.lumen.api.handler.ExpressionHandler;
import dev.lumenlang.lumen.api.pattern.PatternMeta;
import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Associates a compiled {@link Pattern} with the {@link ExpressionHandler} that should be invoked
 * when the pattern is matched in a {@code set x to <pattern>} context.
 *
 * <p>The optional {@code returnTypeId} declares the type this expression
 * statically produces (e.g. {@code "PLAYER"}, {@code "int"}, {@code "String"}).
 * Tooling can use this to resolve the type of a variable assigned from this
 * expression without executing the handler. Expressions whose return type depends
 * on runtime input may leave it {@code null}.
 *
 * @param pattern      the compiled pattern to match against expression token lists
 * @param handler      the handler invoked on a successful match
 * @param meta         documentation metadata for this pattern
 * @param returnTypeId the type id this expression always returns, or {@code null} if dynamic
 */
public record RegisteredExpression(@NotNull Pattern pattern, @NotNull ExpressionHandler handler,
                                   @NotNull PatternMeta meta,
                                   @Nullable String returnTypeId) {

    public RegisteredExpression(@NotNull Pattern pattern, @NotNull ExpressionHandler handler) {
        this(pattern, handler, PatternMeta.EMPTY, null);
    }
}
