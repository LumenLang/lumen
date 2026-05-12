package dev.lumenlang.lumen.pipeline.language.simulator.prefilter.result;

import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Result of pre-filtering one registered pattern against the input tokens.
 *
 * @param pattern      the candidate pattern
 * @param confidence   pre-filter confidence (0..1) above {@code MIN_PREFILTER_CONFIDENCE}
 * @param matchDetails per-literal match outcomes, in pattern order
 * @param handler      the registered handler object, or null when no handler is present
 */
public record PreFilterScore(@NotNull Pattern pattern, double confidence,
                             @NotNull List<LiteralMatchResult> matchDetails, @Nullable Object handler) {
}
