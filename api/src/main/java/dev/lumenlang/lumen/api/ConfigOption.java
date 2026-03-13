package dev.lumenlang.lumen.api;

import org.jetbrains.annotations.NotNull;

/**
 * Enumerates Lumen configuration options that addons can override.
 *
 * <p>Each option corresponds to a boolean flag in Lumen's {@code config.yml}.
 * Addons return {@link ConfigOverride} instances referencing these options from
 * {@link LumenAddon#configOverrides()}.
 *
 * @see ConfigOverride
 * @see LumenAddon#configOverrides()
 */
public enum ConfigOption {

    /**
     * Controls whether Paper specific APIs are enabled.
     */
    PAPER_ONLY_FEATURES("features", "paper-only-features"),

    /**
     * Controls whether the compiler classpath is stripped for faster compilation.
     *
     * <p>When enabled, Lumen removes unnecessary classes from the compiler's classpath.
     * Addons that need those internal classes should set this to false.
     */
    REDUCE_CLASSPATH("performance", "reduce-classpath"),

    /**
     * Controls whether scripts load immediately on startup instead of waiting for the first tick.
     *
     * <p>When enabled, scripts begin compiling during server startup. Addons that register
     * patterns during the Bukkit plugin enable phase (which runs later) should disable this
     * so scripts wait until all addons are ready.
     */
    ENABLE_ALL_SCRIPTS_IMMEDIATELY("scripts", "enable-all-scripts-immediately-on-startup");

    private final String section;
    private final String key;

    ConfigOption(@NotNull String section, @NotNull String key) {
        this.section = section;
        this.key = key;
    }

    /**
     * Returns the YAML section name that contains this option.
     *
     * @return the section name (e.g. {@code "features"}, {@code "performance"})
     */
    public @NotNull String section() {
        return section;
    }

    /**
     * Returns the YAML key name within its section.
     *
     * @return the config key (e.g. {@code "paper-only-features"})
     */
    public @NotNull String key() {
        return key;
    }
}
