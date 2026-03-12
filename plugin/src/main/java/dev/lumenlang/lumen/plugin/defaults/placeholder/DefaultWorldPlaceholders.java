package dev.lumenlang.lumen.plugin.defaults.placeholder;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.placeholder.PlaceholderRegistrar;
import dev.lumenlang.lumen.api.placeholder.PlaceholderType;
import dev.lumenlang.lumen.api.type.Types;
import org.jetbrains.annotations.NotNull;

/**
 * Registers built-in placeholder properties for the WORLD ref type.
 */
@Registration
@SuppressWarnings("unused")
public class DefaultWorldPlaceholders {

    @Call
    public void register(@NotNull LumenAPI api) {
        PlaceholderRegistrar ph = api.placeholders();

        ph.property(Types.WORLD, "name", "$.getName()");
        ph.property(Types.WORLD, "time", "$.getTime()", PlaceholderType.NUMBER);
        ph.property(Types.WORLD, "weather", "($.hasStorm() ? \"storm\" : \"clear\")");
        ph.defaultProperty(Types.WORLD, "name");
    }
}
