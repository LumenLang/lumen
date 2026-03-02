package net.vansencool.lumen.plugin.defaults.placeholders;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.placeholder.PlaceholderRegistrar;
import net.vansencool.lumen.api.placeholder.PlaceholderType;
import net.vansencool.lumen.api.type.RefTypes;
import org.jetbrains.annotations.NotNull;

/**
 * Registers built-in placeholder properties for the WORLD ref type.
 */
@Registration
@Description("Registers built-in placeholders properties for WORLD")
@SuppressWarnings("unused")
public class DefaultWorldPlaceholders {

    @Call
    public void register(@NotNull LumenAPI api) {
        PlaceholderRegistrar ph = api.placeholders();

        ph.property(RefTypes.WORLD, "name", "$.getName()");
        ph.property(RefTypes.WORLD, "time", "$.getTime()", PlaceholderType.NUMBER);
        ph.property(RefTypes.WORLD, "weather", "($.hasStorm() ? \"storm\" : \"clear\")");
        ph.defaultProperty(RefTypes.WORLD, "name");
    }
}
