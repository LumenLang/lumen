package dev.lumenlang.lumen.api;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Entry point for a Lumen addon.
 *
 * <p>Addons extend Lumen by registering custom statement patterns, block patterns, conditions,
 * type bindings, and event definitions. Every addon must implement this interface and be
 * registered with Lumen before scripts are loaded.
 *
 * <h2>Registration</h2>
 * <p>There are two ways to register an addon:
 * <ul>
 *   <li><b>Plugin-based:</b> Declare {@code depend: [Lumen]} in your {@code plugin.yml}
 *       and call {@link LumenProvider#registerAddon(LumenAddon)} from your plugin's
 *       {@code onLoad()}.</li>
 *   <li><b>Jar-based:</b> Place a jar containing a {@code META-INF/services/} entry for
 *       this interface in the {@code plugins/Lumen/addons/} directory.</li>
 * </ul>
 *
 * @see LumenAPI
 */
public interface LumenAddon {

    /**
     * Returns the human-readable name of this addon.
     *
     * @return the addon name
     */
    @NotNull String name();

    /**
     * Returns a short description of this addon.
     *
     * @return the addon description
     */
    @NotNull String description();

    /**
     * Returns the version string of this addon.
     *
     * @return the addon version
     */
    @NotNull String version();

    /**
     * Called before {@link #onEnable(LumenAPI)}.
     *
     * <p>Use this to perform early initialization.
     */
    default void onLoad() {
    }

    /**
     * Called when Lumen enables this addon.
     *
     * @param api the API handle for this addon
     */
    void onEnable(@NotNull LumenAPI api);

    /**
     * Called when Lumen shuts down.
     */
    default void onDisable() {
    }

    /**
     * Returns a list of configuration overrides that this addon requires.
     *
     * <p>Each {@link ConfigOverride} specifies which option to change, whether to enable
     * or disable it, and how long the override should persist.
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
     * <p>The default implementation returns an empty list.
     *
     * @return the list of config overrides, never null
     * @see ConfigOverride
     * @see ConfigOption
     */
    default @NotNull List<ConfigOverride> configOverrides() {
        return List.of();
    }

    /**
     * Returns a list of string configuration overrides this addon wants to apply on load.
     *
     * <p>The default implementation returns an empty list.
     *
     * @return the list of string config overrides, never null
     * @see StringConfigOverride
     * @see StringConfigOption
     */
    default @NotNull List<StringConfigOverride> stringConfigOverrides() {
        return List.of();
    }
}
