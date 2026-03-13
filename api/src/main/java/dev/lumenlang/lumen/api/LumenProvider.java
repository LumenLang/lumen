package dev.lumenlang.lumen.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Static entry point for accessing the Lumen API from addon plugins.
 *
 * <h2>Usage from a Bukkit plugin addon</h2>
 * <pre>{@code
 * public class MyPlugin extends JavaPlugin {
 *
 *     public void onEnable() {
 *         // Register your addon with Lumen. This will call your addon's onEnable() with the API handle.
 *         LumenProvider.registerAddon(new MyAddon());
 *     }
 * }
 * }</pre>
 *
 * <h2>Direct API access (no LumenAddon)</h2>
 * <pre>{@code
 * LumenAPI api = LumenProvider.api();
 * if (api != null) {
 *     api.patterns().statement("heal %who:PLAYER%", (line, ctx, out) ->
 *         out.line(ctx.java("who") + ".setHealth(20);")
 *     );
 * }
 * }</pre>
 *
 * <h2>Important</h2>
 * <p>Script loading is deferred to the first server tick. This means any addon that
 * registers patterns, conditions, events, or types during its {@code onEnable()} is
 * guaranteed to have those definitions available before the first script compiles.
 *
 * @see LumenAPI
 * @see LumenAddon
 */
public final class LumenProvider {

    private static @Nullable LumenAPI api;
    private static @Nullable AddonRegistrar registrar;

    private LumenProvider() {
    }

    /**
     * Returns the global {@link LumenAPI} handle.
     *
     * <p>Returns {@code null} if Lumen has not yet finished its {@code onEnable()}.
     * Plugin-based addons that declare {@code depend: [Lumen]} in their {@code plugin.yml}
     * are guaranteed that this returns non-null in their own {@code onEnable()}.
     *
     * @return the API handle, or {@code null} if Lumen is not yet ready
     */
    public static @Nullable LumenAPI api() {
        return api;
    }

    /**
     * Registers a plugin-based addon with Lumen.
     *
     * <p>If Lumen has already finished enabling, the addon's
     * {@link LumenAddon#onEnable(LumenAPI)} is called immediately. Otherwise
     * it is queued and will be enabled when Lumen finishes initializing.
     *
     * <p>Call this from your plugin's {@code onLoad()} after declaring
     * {@code depend: [Lumen]} in your {@code plugin.yml}.
     *
     * @param addon the addon to register
     * @throws IllegalStateException if Lumen has not loaded at all
     */
    public static void registerAddon(@NotNull LumenAddon addon) {
        if (registrar == null) {
            throw new IllegalStateException("Lumen is not loaded  -  cannot register addons");
        }
        registrar.register(addon);
    }

    /**
     * Internal callback used by Lumen to wire addon registration.
     * <b>Not part of the public API contract. Do not call from addons.</b>
     *
     * @param apiInstance    the API implementation
     * @param addonRegistrar the internal addon registrar callback
     */
    public static void init(@NotNull LumenAPI apiInstance, @NotNull AddonRegistrar addonRegistrar) {
        api = apiInstance;
        registrar = addonRegistrar;
    }

    /**
     * Internal cleanup called when Lumen disables.
     * <b>Not part of the public API contract. Do not call from addons.</b>
     */
    public static void teardown() {
        api = null;
        registrar = null;
    }

    /**
     * Internal functional interface for addon registration delegation.
     * <b>Not part of the public API contract.</b>
     */
    @FunctionalInterface
    public interface AddonRegistrar {

        /**
         * @param addon the addon to register
         */
        void register(@NotNull LumenAddon addon);
    }
}
