package dev.lumenlang.lumen.plugin.defaults.placeholder;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.placeholder.PlaceholderRegistrar;
import dev.lumenlang.lumen.api.placeholder.PlaceholderType;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
import org.jetbrains.annotations.NotNull;

/**
 * Registers built-in placeholder properties for the WORLD type.
 */
@Registration
@SuppressWarnings("unused")
public class DefaultWorldPlaceholders {

    @Call
    public void register(@NotNull LumenAPI api) {
        PlaceholderRegistrar ph = api.placeholders();

        ph.property(MinecraftTypes.WORLD, "name", "$.getName()", PlaceholderType.STRING);
        ph.property(MinecraftTypes.WORLD, "time", "$.getTime()", PlaceholderType.NUMBER);
        ph.property(MinecraftTypes.WORLD, "weather", "($.hasStorm() ? \"storm\" : \"clear\")", PlaceholderType.STRING);
        ph.defaultProperty(MinecraftTypes.WORLD, "name");
    }
}
