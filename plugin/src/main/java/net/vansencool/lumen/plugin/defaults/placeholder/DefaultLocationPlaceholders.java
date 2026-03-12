package net.vansencool.lumen.plugin.defaults.placeholder;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.placeholder.PlaceholderRegistrar;
import net.vansencool.lumen.api.placeholder.PlaceholderType;
import net.vansencool.lumen.api.type.Types;
import org.jetbrains.annotations.NotNull;

/**
 * Registers built-in placeholder properties for the LOCATION ref type.
 */
@Registration
@SuppressWarnings("unused")
public class DefaultLocationPlaceholders {

    @Call
    public void register(@NotNull LumenAPI api) {
        PlaceholderRegistrar ph = api.placeholders();
        ph.property(Types.LOCATION, "x", "$.getBlockX()", PlaceholderType.NUMBER);
        ph.property(Types.LOCATION, "y", "$.getBlockY()", PlaceholderType.NUMBER);
        ph.property(Types.LOCATION, "z", "$.getBlockZ()", PlaceholderType.NUMBER);
        ph.property(Types.LOCATION, "world", "$.getWorld().getName()");
        ph.defaultProperty(Types.LOCATION, "world");
    }
}
