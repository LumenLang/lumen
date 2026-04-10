package dev.lumenlang.lumen.plugin.defaults.placeholder;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.placeholder.PlaceholderRegistrar;
import dev.lumenlang.lumen.api.placeholder.PlaceholderType;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
import org.jetbrains.annotations.NotNull;

/**
 * Registers built-in placeholder properties for the LOCATION type.
 */
@Registration
@SuppressWarnings("unused")
public class DefaultLocationPlaceholders {

    @Call
    public void register(@NotNull LumenAPI api) {
        PlaceholderRegistrar ph = api.placeholders();
        ph.property(MinecraftTypes.LOCATION, "x", "$.getBlockX()", PlaceholderType.NUMBER);
        ph.property(MinecraftTypes.LOCATION, "y", "$.getBlockY()", PlaceholderType.NUMBER);
        ph.property(MinecraftTypes.LOCATION, "z", "$.getBlockZ()", PlaceholderType.NUMBER);
        ph.property(MinecraftTypes.LOCATION, "world", "$.getWorld().getName()");
        ph.defaultProperty(MinecraftTypes.LOCATION, "world");
    }
}
