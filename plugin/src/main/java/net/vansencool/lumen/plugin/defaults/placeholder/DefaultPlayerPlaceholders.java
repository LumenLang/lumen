package net.vansencool.lumen.plugin.defaults.placeholder;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.placeholder.PlaceholderRegistrar;
import net.vansencool.lumen.api.placeholder.PlaceholderType;
import net.vansencool.lumen.api.type.RefTypes;
import org.jetbrains.annotations.NotNull;

/**
 * Registers built-in placeholder properties for the PLAYER ref type.
 */
@Registration
@SuppressWarnings("unused")
public class DefaultPlayerPlaceholders {

    @Call
    public void register(@NotNull LumenAPI api) {
        PlaceholderRegistrar ph = api.placeholders();

        ph.property(RefTypes.PLAYER, "name", "$.getName()");
        ph.property(RefTypes.PLAYER, "health", "$.getHealth()", PlaceholderType.NUMBER);
        ph.property(RefTypes.PLAYER, "food", "$.getFoodLevel()", PlaceholderType.NUMBER);
        ph.property(RefTypes.PLAYER, "level", "$.getLevel()", PlaceholderType.NUMBER);
        ph.property(RefTypes.PLAYER, "xp_level", "$.getLevel()", PlaceholderType.NUMBER);
        ph.property(RefTypes.PLAYER, "world", "$.getWorld().getName()");
        ph.property(RefTypes.PLAYER, "x", "$.getLocation().getBlockX()", PlaceholderType.NUMBER);
        ph.property(RefTypes.PLAYER, "y", "$.getLocation().getBlockY()", PlaceholderType.NUMBER);
        ph.property(RefTypes.PLAYER, "z", "$.getLocation().getBlockZ()", PlaceholderType.NUMBER);
        ph.property(RefTypes.PLAYER, "gamemode", "$.getGameMode().name()");
        ph.property(RefTypes.PLAYER, "uuid", "$.getUniqueId().toString()");
        ph.property(RefTypes.PLAYER, "ip", "$.getAddress().getHostString()");
        ph.defaultProperty(RefTypes.PLAYER, "name");
    }
}
