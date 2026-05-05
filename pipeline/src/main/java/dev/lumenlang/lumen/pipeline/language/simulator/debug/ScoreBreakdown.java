package dev.lumenlang.lumen.pipeline.language.simulator.debug;

import org.jetbrains.annotations.NotNull;

/**
 * Per-candidate score components surfaced when verbosity is at least
 * {@link Verbosity#BREAKDOWN}.
 *
 * @param literalCoverage  fraction of required pattern literals matched in input
 * @param exactness        fraction of matched literals that matched without fuzzy distance
 * @param positionAccuracy fraction of literal pairs whose positions are in pattern order
 * @param tokenCoverage    fraction of input tokens consumed by literal matches
 * @param firstMultiplier  multiplier applied for first-token match outcome
 * @param base             weighted sum of the four coverage components before the multiplier
 * @param finalScore       final pre-filter confidence after multiplier and clamping
 */
public record ScoreBreakdown(double literalCoverage, double exactness, double positionAccuracy,
                             double tokenCoverage, double firstMultiplier, double base,
                             double finalScore) {

    /**
     * Single-line {@code key=value} dump for sink rendering.
     */
    public @NotNull String oneLine() {
        return String.format("lit=%.2f exact=%.2f pos=%.2f tok=%.2f base=%.3f firstMul=%.2f final=%.3f", literalCoverage, exactness, positionAccuracy, tokenCoverage, base, firstMultiplier, finalScore);
    }
}
