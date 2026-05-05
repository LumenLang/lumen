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
     * Per-stage timing in milliseconds.
     */
    TIMING(1),
    /**
     * Sub-stage timing inside each pattern's tryMatch, broken down by phase
     * (level-0 match, sandbox, typo lookup, BFS combinations, reorder fallback).
     */
    DEEP_TIMING(2),
    /**
     * Final ranking line for the run.
     */
    RESULT(3),
    /**
     * Each surviving suggestion with its final confidence.
     */
    RANKED(4),
    /**
     * Every scored candidate, including ones below the cut.
     */
    SCORED(5),
    /**
     * Component breakdown of each candidate's score.
     */
    BREAKDOWN(6),
    /**
     * Per-candidate issue derivation steps.
     */
    ISSUES(7),
    /**
     * Per-token alignment between input and pattern slots.
     */
    MATCH(8),
    /**
     * Per-binding parse attempts with success or rejection reason.
     */
    BIND(9),
    /**
     * Candidate generation step including pruning decisions.
     */
    CANDIDATES(10),
    /**
     * Internal scorer state and walk steps.
     */
    DEEP(11);

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
