package dev.lumenlang.lumen.api;

import org.jetbrains.annotations.NotNull;

/**
 * Enumerates Lumen configuration options that addons can override.
 *
 * @see ConfigOverride
 * @see LumenAddon#configOverrides()
 */
public enum ConfigOption {

    /**
     * Controls whether Paper specific APIs are enabled.
     */
    PAPER_ONLY_FEATURES("features.paper-only-features"),

    /**
     * Controls whether the compiler classpath is stripped for faster compilation.
     *
     * <p>When enabled, Lumen removes unnecessary classes from the compiler's classpath.
     * Addons that need those internal classes should set this to false.
     */
    REDUCE_CLASSPATH("performance.reduce-classpath"),

    /**
     * Controls whether scripts load immediately on startup instead of waiting for the first tick.
     *
     * <p>When enabled, scripts begin compiling during server startup. Addons that register
     * patterns during the Bukkit plugin enable phase (which runs later) should disable this
     * so scripts wait until all addons are ready.
     */
    ENABLE_ALL_SCRIPTS_IMMEDIATELY("scripts.enable-all-scripts-immediately-on-startup"),

    /**
     * Controls whether the experimental code transform system runs after code generation.
     *
     * <p>When enabled, registered code transformers can inspect and modify the emitted
     * Java source. Transformers may remove, replace, or insert lines based on their
     * own logic.
     */
    CODE_TRANSFORM("language.experimental.code-transform"),

    /**
     * Controls whether raw Java blocks are allowed inside scripts.
     *
     * <p>When enabled, scripts can contain {@code java:} blocks whose contents are
     * emitted directly into the compiled class without any transformation.
     */
    RAW_JAVA("language.experimental.raw-java"),

    /**
     * Controls whether inventory hot reload is enabled.
     *
     * <p>When enabled, if a player already has an inventory open and its script is reloaded,
     * the contents and title are replaced in place instead of closing and reopening the GUI.
     */
    INVENTORY_HOT_RELOAD("features.inventories.hot-reload");

    private final String path;

    ConfigOption(@NotNull String path) {
        this.path = path;
    }

    /**
     * Returns the full dot separated path to this option in {@code config.yml}.
     *
     * @return the config path
     */
    public @NotNull String path() {
        return path;
    }
}
