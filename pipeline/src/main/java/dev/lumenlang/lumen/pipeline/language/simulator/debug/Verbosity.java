package dev.lumenlang.lumen.pipeline.language.simulator.debug;

import org.jetbrains.annotations.NotNull;

/**
 * Debug verbosity ladder for the pattern simulator. A level implies every lower level.
 */
public enum Verbosity {

    /**
     * No debug emission.
     */
    OFF(0),
    /**
     * Final ranking line for the run.
     */
    RESULT(1),
    /**
     * Each surviving suggestion with its final confidence.
     */
    RANKED(2),
    /**
     * Every scored candidate, including ones below the cut.
     */
    SCORED(3),
    /**
     * Component breakdown of each candidate's score.
     */
    BREAKDOWN(4),
    /**
     * Per-candidate issue derivation steps.
     */
    ISSUES(5),
    /**
     * Per-token alignment between input and pattern slots.
     */
    MATCH(6),
    /**
     * Per-binding parse attempts with success or rejection reason.
     */
    BIND(7),
    /**
     * Candidate generation step including pruning decisions.
     */
    CANDIDATES(8),
    /**
     * Per-stage timing in milliseconds.
     */
    TIMING(9),
    /**
     * Internal scorer state and walk steps.
     */
    DEEP(10);

    private final int rank;

    Verbosity(int rank) {
        this.rank = rank;
    }

    /**
     * Numeric rank, low to high.
     */
    public int rank() {
        return rank;
    }

    /**
     * {@code true} when this verbosity is at least as detailed as {@code other}.
     */
    public boolean atLeast(@NotNull Verbosity other) {
        return rank >= other.rank;
    }
}
