package dev.lumenlang.lumen.api;

import org.jetbrains.annotations.NotNull;

/**
 * Describes an addon's request to override a Lumen string configuration option.
 *
 * @param option      the configuration option to override
 * @param value       the desired string value
 * @param persistence how long the override should last
 * @param reason      a short human readable explanation shown in the server log
 */
public record StringConfigOverride(
        @NotNull StringConfigOption option,
        @NotNull String value,
        @NotNull ConfigOverride.Persistence persistence,
        @NotNull String reason
) {

    /**
     * Starts building an override for the given option with the given value.
     *
     * @param option the option to override
     * @param value  the desired string value
     * @return a builder to set persistence and reason
     */
    public static @NotNull Builder of(@NotNull StringConfigOption option, @NotNull String value) {
        return new Builder(option, value);
    }

    /**
     * Intermediate builder for choosing persistence level and providing a reason.
     *
     * @param option the configuration option
     * @param value  the desired string value
     */
    public record Builder(@NotNull StringConfigOption option, @NotNull String value) {

        /**
         * Creates an override that lasts only until the configuration is reloaded.
         *
         * @param reason a short human readable explanation
         * @return the config override
         */
        public @NotNull StringConfigOverride session(@NotNull String reason) {
            return new StringConfigOverride(option, value, ConfigOverride.Persistence.SESSION, reason);
        }

        /**
         * Creates an override that persists across configuration reloads but is lost
         * on server restart.
         *
         * @param reason a short human readable explanation
         * @return the config override
         */
        public @NotNull StringConfigOverride lastingSession(@NotNull String reason) {
            return new StringConfigOverride(option, value, ConfigOverride.Persistence.LASTING_SESSION, reason);
        }

        /**
         * Creates an override that is written to {@code config.yml} and persists across
         * server restarts.
         *
         * @param reason a short human readable explanation
         * @return the config override
         */
        public @NotNull StringConfigOverride permanent(@NotNull String reason) {
            return new StringConfigOverride(option, value, ConfigOverride.Persistence.PERMANENT, reason);
        }
    }
}
