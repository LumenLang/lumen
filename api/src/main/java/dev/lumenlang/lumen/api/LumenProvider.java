package dev.lumenlang.lumen.api;

import dev.lumenlang.lumen.api.bus.EventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Static entry point for accessing the Lumen API from addon plugins.
 *
 * <h2>Usage from a Bukkit plugin addon</h2>
 * <pre>{@code
 * public class MyPlugin extends JavaPlugin {
 *
 *     @Override
 *     // Please don't use onEnable() for addon registration - see the note below. Use onLoad() instead.
 *     public void onLoad() {
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
 *     api.patterns().statement("heal %who:PLAYER%", ctx ->
 *         ctx.out().line(ctx.java("who") + ".setHealth(20);")
 *     );
 * }
 * }</pre>
 *
 * <h2>Important</h2>
 * <p>Do not register addons from your plugin's {@code onEnable()} method. Scripts may have already been loaded by then (depending on the configuration), and your addon will miss the chance to register patterns before script compilation.
 *
 * @see LumenAPI
 * @see LumenAddon
 */
public final class LumenProvider {

    private static @Nullable LumenAPI api;
    private static @Nullable AddonRegistrar registrar;
    private static @Nullable EventBus bus;

    private LumenProvider() {
    }

    /**
     * Returns the global {@link LumenAPI} handle.
     *
     * @return the API handle, or {@code null} if Lumen is not yet ready
     * @throws IllegalStateException if Lumen is not yet initialized
     */
    public static @NotNull LumenAPI api() {
        if (api == null) throw new IllegalStateException("Lumen is not initialized yet");
        return api;
    }

    /**
     * Registers a plugin-based addon with Lumen.
     *
     * @param addon the addon to register
     * @throws IllegalStateException if Lumen is not yet initialized
     */
    public static void registerAddon(@NotNull LumenAddon addon) {
        if (registrar == null) {
            throw new IllegalStateException("Lumen is not initialized yet.");
        }
        registrar.register(addon);
    }

    /**
     * Returns the global {@link EventBus} for subscribing to and posting Lumen events.
     *
     * @return the event bus
     * @throws IllegalStateException if Lumen is not yet initialized
     */
    public static @NotNull EventBus bus() {
        if (bus == null) throw new IllegalStateException("Event bus is not initialized yet");
        return bus;
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
     * Initializes the global event bus.
     * <b>Not part of the public API contract. Do not call from addons.</b>
     *
     * @param eventBus the event bus implementation
     */
    public static void initBus(@NotNull EventBus eventBus) {
        bus = eventBus;
    }

    /**
     * Internal cleanup called when Lumen disables.
     * <b>Not part of the public API contract. Do not call from addons.</b>
     */
    public static void teardown() {
        api = null;
        registrar = null;
        bus = null;
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
