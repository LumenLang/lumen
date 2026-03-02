package net.vansencool.lumen.api;

import org.jetbrains.annotations.NotNull;

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
 *       {@code onEnable()}. Script loading is deferred to the first server tick, so all
 *       plugin addons are registered before any scripts compile.</li>
 *   <li><b>Jar-based:</b> Place a jar containing a {@code META-INF/services/} entry for
 *       this interface in the {@code plugins/Lumen/addons/} directory.</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Lumen discovers (jar-based) or receives (plugin-based) the addon.</li>
 *   <li>{@link #onEnable(LumenAPI)} is called with a fully populated {@link LumenAPI} handle.</li>
 *   <li>The addon registers its patterns, conditions, events, etc.</li>
 *   <li>Scripts are loaded; definitions contributed by the addon are available to all scripts.</li>
 *   <li>When Lumen shuts down, {@link #onDisable()} is called.</li>
 * </ol>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * public class MyAddon implements LumenAddon {
 *
 *     public @NotNull String name()        { return "MyAddon"; }
 *     public @NotNull String description() { return "Adds cool stuff"; }
 *     public @NotNull String version()     { return "1.0.0"; }
 *
 *     public void onEnable(LumenAPI api) {
 *         api.patterns().statement("explode %who:PLAYER%", (line, ctx, out) ->
 *             out.line(ctx.java("who") + ".getWorld().createExplosion(" +
 *                      ctx.java("who") + ".getLocation(), 4F);")
 *         );
 *     }
 * }
 * }</pre>
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
     * Called when Lumen enables this addon.
     *
     * <p>Use the supplied {@link LumenAPI} handle to register patterns, conditions,
     * type bindings, event definitions, and ref types.
     *
     * @param api the API handle for this addon
     */
    void onEnable(@NotNull LumenAPI api);

    /**
     * Called when Lumen shuts down.
     *
     * <p>Override this to clean up any resources your addon allocated. The default
     * implementation does nothing.
     */
    default void onDisable() {
    }
}
