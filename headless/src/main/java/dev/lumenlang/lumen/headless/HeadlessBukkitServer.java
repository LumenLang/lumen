package dev.lumenlang.lumen.headless;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Injects a minimal mock {@link Server} into {@link Bukkit} so that static
 * Bukkit calls made during registration (e.g. {@code getBukkitVersion()})
 * resolve without a real server.
 */
public final class HeadlessBukkitServer {

    private static final Logger LOGGER = Logger.getLogger("HeadlessLumen");

    private HeadlessBukkitServer() {
    }

    /**
     * Creates a proxy {@link Server} and installs it via {@link Bukkit#setServer(Server)}.
     */
    public static void install() {
        PluginManager pluginManager = (PluginManager) Proxy.newProxyInstance(
                PluginManager.class.getClassLoader(),
                new Class[]{PluginManager.class},
                (obj, method, args) -> defaultReturn(method.getReturnType()));

        Server server = (Server) Proxy.newProxyInstance(
                Server.class.getClassLoader(),
                new Class[]{Server.class},
                (obj, method, args) -> switch (method.getName()) {
                    case "getBukkitVersion" -> "1.20-R0.1-SNAPSHOT";
                    case "getName" -> "HeadlessLumen";
                    case "getVersion" -> "HeadlessLumen 1.0";
                    case "getLogger" -> LOGGER;
                    case "getPluginManager" -> pluginManager;
                    case "isPrimaryThread" -> true;
                    default -> defaultReturn(method.getReturnType());
                });

        Bukkit.setServer(server);
    }

    private static Object defaultReturn(@NotNull Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0f;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return (char) 0;
        if (type == String.class) return "";
        if (Collection.class.isAssignableFrom(type)) return Collections.emptyList();
        if (Map.class.isAssignableFrom(type)) return Collections.emptyMap();
        return null;
    }
}
