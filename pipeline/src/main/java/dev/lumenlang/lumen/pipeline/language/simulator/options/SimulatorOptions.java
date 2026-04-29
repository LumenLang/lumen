package dev.lumenlang.lumen.pipeline.language.simulator.options;

import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;

/**
 * Immutable bag of {@link SimulatorOption} overrides passed into the pattern simulator.
 *
 * <p>Use {@link #defaults()} for the standard configuration or {@link #builder()} to
 * override individual options. Validation happens at {@code .set(...)}; out-of-range
 * or wrong-kind values throw immediately.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * SimulatorOptions opts = SimulatorOptions.builder()
 *     .set(SimulatorOption.MAX_REMOVAL_DEPTH, 5)
 *     .set(SimulatorOption.TYPO_PENALTY, 0.10)
 *     .build();
 * }</pre>
 */
public final class SimulatorOptions {

    private static final SimulatorOptions DEFAULTS = new SimulatorOptions(new EnumMap<>(SimulatorOption.class));

    private final @NotNull Map<SimulatorOption, Double> overrides;

    private SimulatorOptions(@NotNull Map<SimulatorOption, Double> overrides) {
        this.overrides = overrides;
    }

    /**
     * Returns the standard configuration with no overrides.
     */
    public static @NotNull SimulatorOptions defaults() {
        return DEFAULTS;
    }

    /**
     * Returns a fresh builder seeded with default values.
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Returns the value for an integer option.
     *
     * @param option an option whose kind is {@link SimulatorOption.Kind#INT}
     * @throws IllegalArgumentException if the option's kind is not {@link SimulatorOption.Kind#INT}
     */
    public int intValue(@NotNull SimulatorOption option) {
        if (option.kind() != SimulatorOption.Kind.INT) throw new IllegalArgumentException(option + " is not an int option");
        Double v = overrides.get(option);
        return v != null ? v.intValue() : (int) option.defaultValue();
    }

    /**
     * Returns the value for a double option.
     *
     * @param option an option whose kind is {@link SimulatorOption.Kind#DOUBLE}
     * @throws IllegalArgumentException if the option's kind is not {@link SimulatorOption.Kind#DOUBLE}
     */
    public double doubleValue(@NotNull SimulatorOption option) {
        if (option.kind() != SimulatorOption.Kind.DOUBLE) throw new IllegalArgumentException(option + " is not a double option");
        Double v = overrides.get(option);
        return v != null ? v : option.defaultValue();
    }

    /**
     * Mutable builder for {@link SimulatorOptions}. Validates each override at {@code .set(...)}.
     */
    public static final class Builder {

        private final @NotNull Map<SimulatorOption, Double> values = new EnumMap<>(SimulatorOption.class);

        private Builder() {
        }

        /**
         * Sets an integer option.
         *
         * @param option the option to override; must be of kind {@link SimulatorOption.Kind#INT}
         * @param value  the new value; must be within the option's valid range
         * @throws IllegalArgumentException if the option is not int-kind or value is out of range
         */
        public @NotNull Builder set(@NotNull SimulatorOption option, int value) {
            if (option.kind() != SimulatorOption.Kind.INT) throw new IllegalArgumentException(option + " expects a double, got int");
            Range range = option.range();
            if (!range.contains(value)) throw new IllegalArgumentException(option + " value " + value + " out of range [" + (long) range.min() + ", " + (range.max() == Double.POSITIVE_INFINITY ? "inf" : Long.toString((long) range.max())) + "]");
            values.put(option, (double) value);
            return this;
        }

        /**
         * Sets a double option.
         *
         * @param option the option to override; must be of kind {@link SimulatorOption.Kind#DOUBLE}
         * @param value  the new value; must be within the option's valid range
         * @throws IllegalArgumentException if the option is not double-kind or value is out of range
         */
        public @NotNull Builder set(@NotNull SimulatorOption option, double value) {
            if (option.kind() != SimulatorOption.Kind.DOUBLE) throw new IllegalArgumentException(option + " expects an int, got double");
            Range range = option.range();
            if (!range.contains(value)) throw new IllegalArgumentException(option + " value " + value + " out of range [" + range.min() + ", " + (range.max() == Double.POSITIVE_INFINITY ? "inf" : Double.toString(range.max())) + "]");
            values.put(option, value);
            return this;
        }

        /**
         * Builds the immutable options snapshot.
         */
        public @NotNull SimulatorOptions build() {
            return new SimulatorOptions(new EnumMap<>(values));
        }
    }
}
