package dev.lumenlang.lumen.pipeline.language.simulator.options;

import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator;
import org.jetbrains.annotations.NotNull;

/**
 * Tunable knobs for {@link PatternSimulator}.
 *
 * <p>Each option carries a numeric kind ({@link Kind#INT} or {@link Kind#DOUBLE}),
 * a default value, and a valid {@link Range}. Use {@link SimulatorOptions#builder()}
 * to override defaults.
 */
public enum SimulatorOption {

    /**
     * Maximum number of input tokens BFS will try removing in one search.
     *
     * <p>Higher values catch sentences with more extra junk but cost combinatorial time.
     */
    MAX_REMOVAL_DEPTH(Kind.INT, 3, Range.atLeast(0)),

    /**
     * Top-N pre-filter survivors that get the expensive analysis pass.
     *
     * <p>Lower is faster, higher catches near matches the pre-filter scored low.
     */
    MAX_CANDIDATES(Kind.INT, 10, Range.atLeast(1)),

    /**
     * Maximum suggestions returned to the caller.
     */
    MAX_SUGGESTIONS(Kind.INT, 2, Range.atLeast(0)),

    /**
     * Maximum input length for the reorder fallback. Inputs longer than this skip
     * reorder analysis because shape matching is exponential.
     */
    SHAPE_MATCH_TOKEN_LIMIT(Kind.INT, 20, Range.atLeast(1)),

    /**
     * Hard cap on BFS combinations evaluated at each removal depth.
     *
     * <p>Stops pathological inputs from blowing up search time.
     */
    MAX_COMBINATIONS_PER_LEVEL(Kind.INT, 300, Range.atLeast(1)),

    /**
     * Minimum pre-filter confidence required to reach the analysis pass.
     */
    MIN_PREFILTER_CONFIDENCE(Kind.DOUBLE, 0.15, Range.zeroToOne()),

    /**
     * Pre-filter weight for fraction of pattern's required literals found in input.
     *
     * <p>The four {@code WEIGHT_*} options are intended to sum to roughly 1.0.
     */
    WEIGHT_LITERAL_COVERAGE(Kind.DOUBLE, 0.40, Range.zeroToOne()),

    /**
     * Pre-filter weight for fraction of matched literals that matched exactly.
     */
    WEIGHT_EXACTNESS(Kind.DOUBLE, 0.25, Range.zeroToOne()),

    /**
     * Pre-filter weight for whether matched literals appear in the right order.
     */
    WEIGHT_POSITION(Kind.DOUBLE, 0.20, Range.zeroToOne()),

    /**
     * Pre-filter weight for fraction of input tokens claimed by some literal.
     */
    WEIGHT_TOKEN_COVERAGE(Kind.DOUBLE, 0.15, Range.zeroToOne()),

    /**
     * Confidence penalty applied per token removed by BFS.
     */
    REMOVAL_PENALTY(Kind.DOUBLE, 0.08, Range.zeroToOne()),

    /**
     * Confidence penalty applied per typo correction.
     */
    TYPO_PENALTY(Kind.DOUBLE, 0.05, Range.zeroToOne()),

    /**
     * Confidence multiplier applied when the input's first non-symbol token does
     * not match the pattern's first required literal.
     */
    FIRST_TOKEN_MISS_MULTIPLIER(Kind.DOUBLE, 0.5, Range.zeroToOne()),

    /**
     * Minimum confidence floor for a reorder suggestion that validated against
     * the real matcher.
     */
    VALIDATED_REORDER_FLOOR(Kind.DOUBLE, 0.75, Range.zeroToOne()),

    /**
     * Confidence multiplier applied when a sandboxed handler invocation throws.
     */
    SANDBOX_REJECTED_PENALTY(Kind.DOUBLE, 0.75, Range.zeroToOne()),

    /**
     * Minimum pre-filter confidence required to attempt the reorder fallback. Patterns that
     * scored below this threshold during the pre-filter pass skip reorder analysis entirely,
     * which prevents catch-all patterns (where most parts are placeholders) from emitting
     * spurious reorder suggestions on inputs they barely match.
     */
    REORDER_PREFILTER_FLOOR(Kind.DOUBLE, 0.50, Range.zeroToOne());

    /**
     * Numeric kind a {@link SimulatorOption} accepts.
     */
    public enum Kind {

        /**
         * Integer option, set with {@code .set(option, int)}.
         */
        INT,

        /**
         * Double option, set with {@code .set(option, double)}.
         */
        DOUBLE
    }

    private final @NotNull Kind kind;
    private final double defaultValue;
    private final @NotNull Range range;

    SimulatorOption(@NotNull Kind kind, double defaultValue, @NotNull Range range) {
        this.kind = kind;
        this.defaultValue = defaultValue;
        this.range = range;
    }

    public @NotNull Kind kind() {
        return kind;
    }

    /**
     * Returns the default value as a {@code double}. For {@link Kind#INT} options the
     * value is integral but exposed as a double for uniform storage.
     */
    public double defaultValue() {
        return defaultValue;
    }

    /**
     * Returns the valid range for this option's value.
     */
    public @NotNull Range range() {
        return range;
    }
}
