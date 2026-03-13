package dev.lumenlang.lumen.api;

import org.jetbrains.annotations.NotNull;

/**
 * Describes an addon's request to override a Lumen configuration option.
 *
 * <p>Use the static factory methods {@link #disable(ConfigOption)} and
 * {@link #enable(ConfigOption)} to create a {@link Builder}, then call
 * {@link Builder#session(String)}, {@link Builder#lastingSession(String)},
 * or {@link Builder#permanent(String)} to set the persistence level and reason.
 *
 * <h2>Persistence Levels</h2>
 * <ul>
 *   <li>{@link Persistence#SESSION}: Applied in memory only. Lost when the
 *       configuration is reloaded.</li>
 *   <li>{@link Persistence#LASTING_SESSION}: Applied in memory and re-applied
 *       automatically after configuration reloads. Lost on server restart.</li>
 *   <li>{@link Persistence#PERMANENT}: Written to {@code config.yml} and persisted
 *       across server restarts.</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * @Override
 * public @NotNull List<ConfigOverride> configOverrides() {
 *     return List.of(
 *         ConfigOverride.disable(ConfigOption.PAPER_ONLY_FEATURES)
 *             .permanent("requires Spigot compatibility"),
 *         ConfigOverride.disable(ConfigOption.ENABLE_ALL_SCRIPTS_IMMEDIATELY)
 *             .lastingSession("registers patterns during plugin enable")
 *     );
 * }
 * }</pre>
 *
 * @param option      the configuration option to override
 * @param value       the desired boolean value ({@code true} to enable, {@code false} to disable)
 * @param persistence how long the override should last
 * @param reason      a short human readable explanation shown in the server log
 * @see ConfigOption
 * @see LumenAddon#configOverrides()
 */
public record ConfigOverride(
        @NotNull ConfigOption option,
        boolean value,
        @NotNull Persistence persistence,
        @NotNull String reason
) {

    /**
     * Describes how long a configuration override persists.
     */
    public enum Persistence {

        /**
         * Applied in memory only. Lost when the configuration is reloaded.
         */
        SESSION,

        /**
         * Applied in memory and re-applied automatically after configuration reloads.
         * Lost on server restart.
         */
        LASTING_SESSION,

        /**
         * Written to {@code config.yml}. Persists across server restarts.
         */
        PERMANENT
    }

    /**
     * Starts building an override that disables (sets to {@code false}) the given option.
     *
     * @param option the option to disable
     * @return a builder to set persistence and reason
     */
    public static @NotNull Builder disable(@NotNull ConfigOption option) {
        return new Builder(option, false);
    }

    /**
     * Starts building an override that enables (sets to {@code true}) the given option.
     *
     * @param option the option to enable
     * @return a builder to set persistence and reason
     */
    public static @NotNull Builder enable(@NotNull ConfigOption option) {
        return new Builder(option, true);
    }

    /**
     * Intermediate builder for choosing persistence level and providing a reason.
     *
     * @param option the configuration option
     * @param value  the desired boolean value
     */
    public record Builder(@NotNull ConfigOption option, boolean value) {

        /**
         * Creates an override that lasts only until the configuration is reloaded.
         *
         * @param reason a short human readable explanation
         * @return the config override
         */
        public @NotNull ConfigOverride session(@NotNull String reason) {
            return new ConfigOverride(option, value, Persistence.SESSION, reason);
        }

        /**
         * Creates an override that persists across configuration reloads but is lost
         * on server restart.
         *
         * @param reason a short human readable explanation
         * @return the config override
         */
        public @NotNull ConfigOverride lastingSession(@NotNull String reason) {
            return new ConfigOverride(option, value, Persistence.LASTING_SESSION, reason);
        }

        /**
         * Creates an override that is written to {@code config.yml} and persists across
         * server restarts.
         *
         * @param reason a short human readable explanation
         * @return the config override
         */
        public @NotNull ConfigOverride permanent(@NotNull String reason) {
            return new ConfigOverride(option, value, Persistence.PERMANENT, reason);
        }
    }
}
