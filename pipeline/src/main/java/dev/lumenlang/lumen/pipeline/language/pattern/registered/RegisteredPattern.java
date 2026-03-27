package dev.lumenlang.lumen.pipeline.language.pattern.registered;

import dev.lumenlang.lumen.api.handler.StatementHandler;
import dev.lumenlang.lumen.api.pattern.PatternMeta;
import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * Associates a compiled {@link Pattern} with the {@link StatementHandler} that should be invoked
 * when the pattern is matched during code generation.
 *
 * @param pattern the compiled pattern to match against statement token lists
 * @param handler the handler invoked on a successful match
 * @param meta    documentation metadata for this pattern
 * @see PatternRegistry#statement(String, StatementHandler)
 * @see RegisteredPatternMatch
 */
public record RegisteredPattern(@NotNull Pattern pattern, @NotNull StatementHandler handler,
                                @NotNull PatternMeta meta) {

    public RegisteredPattern(@NotNull Pattern pattern, @NotNull StatementHandler handler) {
        this(pattern, handler, PatternMeta.EMPTY);
    }
}
