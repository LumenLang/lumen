package net.vansencool.lumen.plugin.platform;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for detecting the server platform (Paper, Spigot, or CraftBukkit) at runtime.
 */
@SuppressWarnings("unused")
public final class ServerPlatform {

    private static final boolean PAPER;
    private static final boolean SPIGOT;
    private static final boolean CRAFTBUKKIT;

    static {
        boolean paper = hasClass("com.destroystokyo.paper.PaperConfig") || hasClass("io.papermc.paper.configuration.Configuration");

        boolean spigot = !paper && hasClass("org.spigotmc.SpigotConfig");

        PAPER = paper;
        SPIGOT = spigot;
        CRAFTBUKKIT = !paper && !spigot;
    }

    private static boolean hasClass(@NotNull String name) {
        try {
            Class.forName(name, false, ServerPlatform.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Checks if the server is running Paper, or a fork of Paper that includes the PaperConfig class.
     *
     * @return true if Paper is detected, false otherwise
     */
    public static boolean isPaper() {
        return PAPER;
    }

    /**
     * Checks if the server is running Spigot, or a fork of Spigot that includes the SpigotConfig class.
     *
     * @return true if Spigot is detected, false otherwise
     */
    public static boolean isSpigot() {
        return SPIGOT;
    }

    /**
     * Checks if the server is running CraftBukkit, or a fork of CraftBukkit that does not include PaperConfig or SpigotConfig.
     *
     * @return true if CraftBukkit is detected, false otherwise
     */
    public static boolean isCraftBukkit() {
        return CRAFTBUKKIT;
    }

    /**
     * Gets the name of the server software as reported by Bukkit.getName().
     *
     * @return the server software name
     */
    public static @NotNull String getSoftwareName() {
        return Bukkit.getName();
    }
}
