package dev.lumenlang.lumen.pipeline.language.simulator.score;

import dev.lumenlang.lumen.pipeline.language.match.MatchProgress;
import dev.lumenlang.lumen.pipeline.language.simulator.options.SimulatorOption;
import dev.lumenlang.lumen.pipeline.language.simulator.options.SimulatorOptions;
import org.jetbrains.annotations.NotNull;

/**
 * Confidence-score math for suggestions emitted by the typo and type-mismatch fallback paths.
 */
public final class Confidence {

    private Confidence() {
    }

    /**
     * Confidence for a suggestion produced by the typo-correction path. Drops with each typo
     * applied and is halved (per {@code FIRST_TOKEN_MISS_MULTIPLIER}) when the corrected input's
     * first token still differs from the pattern's first required literal.
     */
    public static double forTypo(int typos, boolean firstTokenMatches, @NotNull SimulatorOptions opts) {
        double base = 1.0 - typos * opts.doubleValue(SimulatorOption.TYPO_PENALTY);
        if (!firstTokenMatches) base *= opts.doubleValue(SimulatorOption.FIRST_TOKEN_MISS_MULTIPLIER);
        return Math.max(0.0, Math.min(1.0, base));
    }

    /**
     * Confidence for a fallback suggestion derived from {@link MatchProgress}. Scales with the
     * fraction of tokens the matcher consumed before failure, with a per-binding-failure penalty.
     */
    public static double forTypeMatch(@NotNull MatchProgress progress, int totalTokens) {
        double fraction = totalTokens > 0 ? (double) (progress.furthestTokenIndex() + 1) / totalTokens : 0.0;
        double base = Math.max(0.20, Math.min(0.85, fraction));
        int failures = progress.bindingFailures().size();
        double penalty = failures * 0.12;
        return Math.max(0.05, base - penalty);
    }
}
