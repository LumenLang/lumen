package dev.lumenlang.lumen.api;

import org.jetbrains.annotations.NotNull;

/**
 * Describes an addon's request to disable a Lumen configuration option.
 *
 * <h2>Permanence</h2>
 * <ul>
 *   <li>When {@code permanent} is {@code true}, the change is written to {@code config.yml}
 *       so it persists across server restarts.</li>
 *   <li>When {@code permanent} is {@code false}, the flag is only overridden in memory for
 *       this server session; the config file is left untouched, will be overwritten on configuration reload.</li>
 * </ul>
 *
 * @param reason    a short human-readable explanation shown in the server log
 * @param permanent whether the change should be persisted to disk
 */
public record DisableSetting(@NotNull String reason, boolean permanent) {

    /**
     * Creates a {@link DisableSetting} that is applied only for the current session.
     *
     * @param reason a short human-readable explanation
     * @return a non-permanent disable setting
     */
    public static @NotNull DisableSetting session(@NotNull String reason) {
        return new DisableSetting(reason, false);
    }

    /**
     * Creates a {@link DisableSetting} that is written to disk and persists across restarts.
     *
     * @param reason a short human-readable explanation
     * @return a permanent disable setting
     */
    public static @NotNull DisableSetting permanent(@NotNull String reason) {
        return new DisableSetting(reason, true);
    }
}
