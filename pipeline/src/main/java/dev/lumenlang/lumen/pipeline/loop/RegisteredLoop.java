package dev.lumenlang.lumen.pipeline.loop;

import dev.lumenlang.lumen.api.handler.LoopHandler;
import dev.lumenlang.lumen.api.pattern.PatternMeta;
import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import org.jetbrains.annotations.NotNull;

/**
 * Associates a compiled {@link Pattern} with the {@link LoopHandler} that generates the
 * iterable Java expression when the pattern matches a loop source fragment.
 *
 * @param pattern the compiled loop source pattern
 * @param handler the handler that generates the iterable expression and element type
 * @param meta    documentation metadata for this loop source
 * @see LoopRegistry
 */
public record RegisteredLoop(@NotNull Pattern pattern, @NotNull LoopHandler handler,
                             @NotNull PatternMeta meta) {
}
