package dev.lumenlang.lumen.headless.sim.snapshot;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Serialised shape of one suggestion captured for snapshot comparison.
 *
 * @param patternRaw  pattern source text exactly as registered
 * @param confidence  rounded to 3 decimals so float jitter does not produce diffs
 * @param issues      one rendered line per issue, in pattern-order, fully describing every field
 */
public record SuggestionSnap(@NotNull String patternRaw, double confidence,
                             @NotNull List<String> issues) {
}
