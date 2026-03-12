package net.vansencool.lumen.plugin.defaults.placeholder;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.placeholder.PlaceholderRegistrar;
import net.vansencool.lumen.api.placeholder.PlaceholderType;
import net.vansencool.lumen.api.type.Types;
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
