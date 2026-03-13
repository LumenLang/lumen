package dev.lumenlang.lumen.plugin.text;

import dev.lumenlang.lumen.pipeline.java.compiled.LumenNullException;
import dev.lumenlang.lumen.pipeline.minicolorize.MiniColorize;
import dev.lumenlang.lumen.plugin.configuration.LumenConfiguration;
import dev.lumenlang.lumen.plugin.minicolorize.BukkitMiniColorizeSerializer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runtime text processing utility for compiled Lumen scripts.
 *
 * <p>All text is processed through Lumen's own MiniColorize system, which supports
 * MiniMessage-style tags ({@code <red>}, {@code <gradient>}, {@code <hover>}, etc.)
 * and is serialized to BungeeCord Chat {@link BaseComponent} arrays.
 *
 * <p>Legacy {@code &} color codes are optionally translated to MiniColorize tags
 * when {@code use-legacy-colors} is enabled in the configuration.
 *
 * @see MiniColorize
 * @see BukkitMiniColorizeSerializer
 * @see LumenConfiguration.Features
 */
@SuppressWarnings("unused")
public final class LumenText {

    private static final MiniColorize MINI_COLORIZE = MiniColorize.standard();
    private static final BukkitMiniColorizeSerializer SERIALIZER = new BukkitMiniColorizeSerializer();

    private LumenText() {
    }

    /**
     * Sends a colorized message to a {@link CommandSender}.
     *
     * @param sender the recipient
     * @param text   the raw text, potentially containing color codes or MiniColorize tags
     */
    public static void send(@Nullable CommandSender sender, @Nullable String text) {
        if (sender == null) {
            throw new LumenNullException("sender", "Cannot send text to null sender");
        }
        if (text == null) {
            throw new LumenNullException("text", "Cannot send null text to " + sender.getName());
        }
        BaseComponent[] components = toComponents(text);
        sender.spigot().sendMessage(components);
    }

    /**
     * Broadcasts a colorized message to all online players and the console.
     *
     * @param text the raw text, potentially containing color codes or MiniColorize tags
     */
    public static void broadcast(@Nullable String text) {
        if (text == null) {
            throw new LumenNullException("text", "Cannot broadcast null text");
        }
        BaseComponent[] components = toComponents(text);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.spigot().sendMessage(components);
        }
        Bukkit.getConsoleSender().spigot().sendMessage(components);
    }

    /**
     * Translates legacy {@code &} color codes and MiniColorize tags into a
     * legacy section-sign ({@code §}) formatted string.
     *
     * <p>This is used for contexts that require plain strings, such as
     * item display names and lore lines.
     *
     * @param text the raw text containing color codes or MiniColorize tags
     * @return the translated string with section-sign color codes
     */
    public static @NotNull String colorize(@Nullable String text) {
        if (text == null) {
            throw new LumenNullException("text", "Cannot colorize null text");
        }
        BaseComponent[] components = toComponents(text);
        return TextComponent.toLegacyText(components);
    }

    /**
     * Sends a title and optional subtitle to a player.
     *
     * @param player   the player to display the title to
     * @param title    the main title text
     * @param subtitle the subtitle text, or null for no subtitle
     * @param fadeIn   fade-in duration in ticks
     * @param stay     stay duration in ticks
     * @param fadeOut  fade-out duration in ticks
     */
    public static void sendTitle(@Nullable Player player, @Nullable String title,
                                 @Nullable String subtitle, int fadeIn, int stay, int fadeOut) {
        if (player == null) {
            throw new LumenNullException("player", "Cannot send title to null player");
        }
        if (title == null) {
            throw new LumenNullException("title", "Cannot send null title to " + player.getName());
        }
        String titleLegacy = colorize(title);
        String subtitleLegacy = subtitle != null ? colorize(subtitle) : "";
        player.sendTitle(titleLegacy, subtitleLegacy, fadeIn, stay, fadeOut);
    }

    /**
     * Sends an action bar message to a player.
     *
     * @param player the player to display the action bar to
     * @param text   the action bar text
     */
    public static void sendActionBar(@Nullable Player player, @Nullable String text) {
        if (player == null) {
            throw new LumenNullException("player", "Cannot send action bar to null player");
        }
        if (text == null) {
            throw new LumenNullException("text", "Cannot send null action bar text to " + player.getName());
        }
        BaseComponent[] components = toComponents(text);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, components);
    }

    /**
     * Processes raw text through MiniColorize into a {@link BaseComponent} array,
     * applying legacy color code translation if enabled.
     *
     * @param text the raw text
     * @return the serialized components
     */
    public static @NotNull BaseComponent[] toComponents(@Nullable String text) {
        if (text == null) {
            throw new LumenNullException("text", "Cannot process null text");
        }
        String processed = text;
        if (LumenConfiguration.FEATURES.USE_LEGACY_COLORS) {
            processed = legacyToMini(ChatColor.translateAlternateColorCodes('&', processed));
        }
        return MINI_COLORIZE.process(processed, SERIALIZER);
    }

    /**
     * Converts section-sign ({@code §}) color codes into MiniColorize tags
     * so they can be parsed by the MiniColorize system.
     *
     * @param text the input text containing {@code §} color codes
     * @return the text with color codes replaced by MiniColorize tags
     */
    private static @NotNull String legacyToMini(@NotNull String text) {
        StringBuilder out = new StringBuilder(text.length());
        char[] chars = text.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == ChatColor.COLOR_CHAR && i + 1 < chars.length) {
                char code = Character.toLowerCase(chars[i + 1]);
                i++;

                switch (code) {
                    case '0' -> out.append("<black>");
                    case '1' -> out.append("<dark_blue>");
                    case '2' -> out.append("<dark_green>");
                    case '3' -> out.append("<dark_aqua>");
                    case '4' -> out.append("<dark_red>");
                    case '5' -> out.append("<dark_purple>");
                    case '6' -> out.append("<gold>");
                    case '7' -> out.append("<gray>");
                    case '8' -> out.append("<dark_gray>");
                    case '9' -> out.append("<blue>");
                    case 'a' -> out.append("<green>");
                    case 'b' -> out.append("<aqua>");
                    case 'c' -> out.append("<red>");
                    case 'd' -> out.append("<light_purple>");
                    case 'e' -> out.append("<yellow>");
                    case 'f' -> out.append("<white>");
                    case 'l' -> out.append("<bold>");
                    case 'm' -> out.append("<strikethrough>");
                    case 'n' -> out.append("<underlined>");
                    case 'o' -> out.append("<italic>");
                    case 'r' -> out.append("<reset>");
                    default -> {
                    }
                }
            } else {
                out.append(chars[i]);
            }
        }

        return out.toString();
    }
}
