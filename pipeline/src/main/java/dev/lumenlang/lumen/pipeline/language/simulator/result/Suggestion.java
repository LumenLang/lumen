package dev.lumenlang.lumen.pipeline.language.simulator.result;

import dev.lumenlang.lumen.pipeline.language.match.MatchProgress;
import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A single simulation result describing a near match and the issues found.
 *
 * @param pattern    the candidate pattern that closely matched the input
 * @param confidence confidence score between 0.0 and 1.0 (0% to 100%)
 * @param issues     the specific issues detected (typos, type mismatches, missing parts)
 * @param progress   match progress from real simulation, or null if not enriched
 */
public record Suggestion(@NotNull Pattern pattern, double confidence, @NotNull List<SuggestionIssue> issues,
                         @Nullable MatchProgress progress) {
}
