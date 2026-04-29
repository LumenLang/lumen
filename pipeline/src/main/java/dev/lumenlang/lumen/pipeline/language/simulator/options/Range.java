package dev.lumenlang.lumen.pipeline.language.simulator.options;

import org.jetbrains.annotations.NotNull;

/**
 * Inclusive numeric range used to validate {@link SimulatorOption} values.
 */
public final class Range {

    private static final Range ZERO_TO_ONE = new Range(0.0, 1.0);

    private final double min;
    private final double max;

    private Range(double min, double max) {
        this.min = min;
        this.max = max;
    }

    /**
     * Returns the inclusive {@code [0.0, 1.0]} range used by ratio-style options.
     */
    public static @NotNull Range zeroToOne() {
        return ZERO_TO_ONE;
    }

    /**
     * Returns an open-ended range with only a lower bound.
     *
     * @param min inclusive lower bound; the upper bound is {@link Double#POSITIVE_INFINITY}
     */
    public static @NotNull Range atLeast(double min) {
        return new Range(min, Double.POSITIVE_INFINITY);
    }

    /**
     * Returns a range with both bounds specified.
     *
     * @param min inclusive lower bound
     * @param max inclusive upper bound; must be greater than or equal to {@code min}
     */
    public static @NotNull Range between(double min, double max) {
        if (max < min) throw new IllegalArgumentException("Range max " + max + " is less than min " + min);
        return new Range(min, max);
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    /**
     * Returns whether the given numeric value falls within this range.
     *
     * @param value candidate value
     */
    public boolean contains(double value) {
        return value >= min && value <= max;
    }
}
