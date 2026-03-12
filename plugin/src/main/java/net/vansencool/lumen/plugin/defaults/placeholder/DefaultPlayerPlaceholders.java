package net.vansencool.lumen.plugin.defaults.placeholder;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.placeholder.PlaceholderRegistrar;
import net.vansencool.lumen.api.placeholder.PlaceholderType;
import net.vansencool.lumen.api.type.Types;
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

        ph.property(Types.PLAYER, "name", "$.getName()");
        ph.property(Types.PLAYER, "health", "$.getHealth()", PlaceholderType.NUMBER);
        ph.property(Types.PLAYER, "food", "$.getFoodLevel()", PlaceholderType.NUMBER);
        ph.property(Types.PLAYER, "level", "$.getLevel()", PlaceholderType.NUMBER);
        ph.property(Types.PLAYER, "xp_level", "$.getLevel()", PlaceholderType.NUMBER);
        ph.property(Types.PLAYER, "world", "$.getWorld().getName()");
        ph.property(Types.PLAYER, "x", "$.getLocation().getBlockX()", PlaceholderType.NUMBER);
        ph.property(Types.PLAYER, "y", "$.getLocation().getBlockY()", PlaceholderType.NUMBER);
        ph.property(Types.PLAYER, "z", "$.getLocation().getBlockZ()", PlaceholderType.NUMBER);
        ph.property(Types.PLAYER, "gamemode", "$.getGameMode().name()");
        ph.property(Types.PLAYER, "uuid", "$.getUniqueId().toString()");
        ph.property(Types.PLAYER, "ip", "$.getAddress().getHostString()");
        ph.defaultProperty(Types.PLAYER, "name");
    }
}
