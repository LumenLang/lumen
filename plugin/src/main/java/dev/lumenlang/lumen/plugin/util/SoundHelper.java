package dev.lumenlang.lumen.plugin.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Sound-related helper methods.
 */
@SuppressWarnings("unused")
public final class SoundHelper {

    private SoundHelper() {
    }

    /**
     * Plays a sound to a player at their location.
     */
    public static void playSound(@NotNull Player player, @NotNull String soundString) {
        try {
            Sound sound = Sound.valueOf(soundString.toUpperCase(Locale.ROOT).replace('.', '_'));
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException __ex) {
            player.playSound(player.getLocation(), soundString, 1.0f, 1.0f);
        }
    }
}
