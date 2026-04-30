package dev.lumenlang.lumen.plugin.addon;

import dev.lumenlang.lumen.api.LumenAddon;
import dev.lumenlang.lumen.pipeline.addon.AddonManager;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.plugin.configuration.LumenConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Hardcoded registry of builtin addons. Each addon is instantiated via reflection
 * if its enabled condition evaluates to true.
 */
public final class BuiltinAddonRegistry {

    private static final Map<String, BooleanSupplier> BUILTIN_ADDONS = new LinkedHashMap<>();

    static {
        BUILTIN_ADDONS.put("dev.lumenlang.lumen.debug.LumenDebugAddon", () -> LumenConfiguration.DEBUG.SERVICE.ENABLED);
    }

    private BuiltinAddonRegistry() {
    }

    /**
     * Instantiates and registers all builtin addons whose enable conditions are true.
     */
    public static void registerAll(@NotNull AddonManager manager) {
        for (Map.Entry<String, BooleanSupplier> entry : BUILTIN_ADDONS.entrySet()) {
            String className = entry.getKey();
            BooleanSupplier enabledCheck = entry.getValue();

            if (!enabledCheck.getAsBoolean()) {
                continue;
            }

            try {
                Class<?> clazz = Class.forName(className);
                LumenAddon addon = (LumenAddon) clazz.getDeclaredConstructor().newInstance();
                manager.registerAddon(addon);
                LumenLogger.info("Registered builtin addon: " + addon.name());
            } catch (ClassNotFoundException e) {
                LumenLogger.warning("Builtin addon " + className + " not found on classpath");
            } catch (Exception e) {
                LumenLogger.severe("Failed to instantiate builtin addon " + className + ": " + e.getMessage());
            }
        }
    }
}
