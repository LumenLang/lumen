package dev.lumenlang.lumen.pipeline.addon;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.LumenAddon;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Manages discovery, registration, and lifecycle of {@link LumenAddon} instances.
 *
 * <p>Addons can be registered in two ways:
 * <ul>
 *   <li><b>Plugin-based:</b> A Bukkit plugin that depends on Lumen calls
 *       {@link #registerAddon(LumenAddon)} during its {@code onEnable()}. The addon
 *       is enabled immediately if the API is already available, or queued until
 *       {@link #enableAll(LumenAPI)} is called.</li>
 *   <li><b>Jar-based:</b> Stand-alone jar files placed in the
 *       {@code plugins/Lumen/addons/} directory are scanned via {@link ServiceLoader}
 *       during {@link #loadAddons(File)}.</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Lumen's {@code onEnable()} creates the manager and calls {@link #loadAddons(File)}.</li>
 *   <li>{@link #enableAll(LumenAPI)} enables all jar-based addons and queued plugin addons.</li>
 *   <li>After Lumen enables, other plugin-based addons may call {@link #registerAddon(LumenAddon)}
 *       and are enabled on the spot.</li>
 *   <li>{@link #disableAll()} calls {@link LumenAddon#onDisable()} on each in reverse order.</li>
 * </ol>
 */
@SuppressWarnings("unused")
public final class AddonManager {

    private final List<LumenAddon> addons = new ArrayList<>();
    private final List<URLClassLoader> loaders = new ArrayList<>();
    private LumenAPI api;

    /**
     * Registers a plugin-based addon.
     *
     * <p>If the API is already available (Lumen has finished enabling), the addon is
     * enabled immediately. Otherwise it is queued and will be enabled when
     * {@link #enableAll(LumenAPI)} is called.
     *
     * @param addon the addon to register
     */
    public void registerAddon(@NotNull LumenAddon addon) {
        addons.add(addon);
        callOnLoad(addon);
        if (api != null) {
            enableSingle(addon);
        }
    }

    /**
     * Scans the given directory for jar files and discovers addons via {@link ServiceLoader}.
     *
     * @param addonsDir the directory to scan (e.g. {@code plugins/Lumen/addons/})
     */
    public void loadAddons(@NotNull File addonsDir) {
        if (!addonsDir.isDirectory()) return;

        File[] jars = addonsDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jars == null) return;

        for (File jar : jars) {
            try {
                URLClassLoader loader = new URLClassLoader(
                        new URL[]{jar.toURI().toURL()},
                        getClass().getClassLoader()
                );
                loaders.add(loader);

                ServiceLoader<LumenAddon> sl = ServiceLoader.load(LumenAddon.class, loader);
                for (LumenAddon addon : sl) {
                    addons.add(addon);
                    callOnLoad(addon);
                    LumenLogger.info("Discovered addon jar: " + addon.name()
                            + " v" + addon.version()
                            + " (" + addon.description() + ")");
                }
            } catch (Exception e) {
                LumenLogger.warning("Failed to load addon jar: " + jar.getName() + "  -  " + e.getMessage());
            }
        }
    }

    /**
     * Enables every registered addon that has not yet been enabled.
     *
     * <p>After this call, any future addons registered via {@link #registerAddon(LumenAddon)}
     * will be enabled immediately.
     *
     * @param api the API handle to pass to each addon
     */
    public void enableAll(@NotNull LumenAPI api) {
        this.api = api;
        for (LumenAddon addon : addons) {
            enableSingle(addon);
        }
    }

    /**
     * Disables all addons in reverse registration order.
     */
    public void disableAll() {
        for (int i = addons.size() - 1; i >= 0; i--) {
            LumenAddon addon = addons.get(i);
            try {
                addon.onDisable();
                LumenLogger.info("Disabled addon: " + addon.name());
            } catch (Exception e) {
                LumenLogger.warning("Failed to disable addon: " + addon.name() + "  -  " + e.getMessage());
            }
        }
        for (URLClassLoader loader : loaders) {
            try {
                loader.close();
            } catch (Exception ignored) {
            }
        }
        addons.clear();
        loaders.clear();
        api = null;
    }

    /**
     * Returns an unmodifiable view of all registered addons.
     *
     * @return the addon list
     */
    public @NotNull List<LumenAddon> addons() {
        return Collections.unmodifiableList(addons);
    }

    /**
     * Returns the addon with the given name, or {@code null} if not found.
     *
     * @param name the addon name
     * @return the addon, or {@code null}
     */
    public @Nullable LumenAddon get(@NotNull String name) {
        for (LumenAddon addon : addons) {
            if (addon.name().equals(name)) return addon;
        }
        return null;
    }

    /**
     * Returns the current API handle, or {@code null} if not yet initialised.
     *
     * @return the API handle
     */
    public @Nullable LumenAPI api() {
        return api;
    }

    private void enableSingle(@NotNull LumenAddon addon) {
        try {
            addon.onEnable(api);
            LumenLogger.info("Enabled addon: " + addon.name() + " v" + addon.version());
        } catch (Exception e) {
            LumenLogger.severe("Error enabling addon " + addon.name() + " v" + addon.version(), e);
        }
    }

    private void callOnLoad(@NotNull LumenAddon addon) {
        try {
            addon.onLoad();
        } catch (Exception e) {
            LumenLogger.warning("Error calling onLoad for addon " + addon.name() + " v" + addon.version() + ": " + e.getMessage());
        }
    }
}
