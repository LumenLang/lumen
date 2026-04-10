package dev.lumenlang.lumen.plugin.defaults.placeholder;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.placeholder.PlaceholderRegistrar;
import dev.lumenlang.lumen.api.placeholder.PlaceholderType;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
import org.jetbrains.annotations.NotNull;

/**
 * Registers built-in placeholder properties for the PLAYER type.
 */
@Registration
@SuppressWarnings("unused")
public class DefaultPlayerPlaceholders {

    @Call
    public void register(@NotNull LumenAPI api) {
        PlaceholderRegistrar ph = api.placeholders();

        ph.property(MinecraftTypes.PLAYER, "name", "$.getName()");
        ph.property(MinecraftTypes.PLAYER, "health", "$.getHealth()", PlaceholderType.NUMBER);
        ph.property(MinecraftTypes.PLAYER, "food", "$.getFoodLevel()", PlaceholderType.NUMBER);
        ph.property(MinecraftTypes.PLAYER, "level", "$.getLevel()", PlaceholderType.NUMBER);
        ph.property(MinecraftTypes.PLAYER, "xp_level", "$.getLevel()", PlaceholderType.NUMBER);
        ph.property(MinecraftTypes.PLAYER, "world", "$.getWorld().getName()");
        ph.property(MinecraftTypes.PLAYER, "x", "$.getLocation().getBlockX()", PlaceholderType.NUMBER);
        ph.property(MinecraftTypes.PLAYER, "y", "$.getLocation().getBlockY()", PlaceholderType.NUMBER);
        ph.property(MinecraftTypes.PLAYER, "z", "$.getLocation().getBlockZ()", PlaceholderType.NUMBER);
        ph.property(MinecraftTypes.PLAYER, "gamemode", "$.getGameMode().name()");
        ph.property(MinecraftTypes.PLAYER, "uuid", "$.getUniqueId().toString()");
        ph.property(MinecraftTypes.PLAYER, "ip", "$.getAddress().getHostString()");
        ph.defaultProperty(MinecraftTypes.PLAYER, "name");
    }
}
