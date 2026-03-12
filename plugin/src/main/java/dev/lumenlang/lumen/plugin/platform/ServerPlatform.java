package dev.lumenlang.lumen.plugin.platform;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Utility class for detecting the server platform (Folia, Paper, Spigot, or CraftBukkit) at runtime.
 */
@SuppressWarnings("unused")
public final class ServerPlatform {

    private static final boolean FOLIA;
    private static final boolean PAPER;
    private static final boolean SPIGOT;
    private static final boolean CRAFTBUKKIT;

    static {
        boolean folia = hasClass("io.papermc.paper.threadedregions.RegionizedServer");
        boolean paper = hasClass("com.destroystokyo.paper.PaperConfig") || hasClass("io.papermc.paper.configuration.Configuration");

        boolean spigot = !paper && hasClass("org.spigotmc.SpigotConfig");

        FOLIA = folia;
        PAPER = folia || paper;
        SPIGOT = spigot;
        CRAFTBUKKIT = !folia && !paper && !spigot;
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
     * Checks if the server is running Folia, or a fork of Folia that includes the RegionizedServer class.
     *
     * @return true if Folia is detected, false otherwise
     */
    public static boolean isFolia() {
        return FOLIA;
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
