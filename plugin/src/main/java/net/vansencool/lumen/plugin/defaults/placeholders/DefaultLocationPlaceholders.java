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
 * Registers built-in placeholder properties for the LOCATION ref type.
 */
@Registration
@Description("Registers built-in placeholders properties for LOCATION")
@SuppressWarnings("unused")
public class DefaultLocationPlaceholders {

    @Call
    public void register(@NotNull LumenAPI api) {
        PlaceholderRegistrar ph = api.placeholders();
        ph.property(RefTypes.LOCATION, "x", "$.getBlockX()", PlaceholderType.NUMBER);
        ph.property(RefTypes.LOCATION, "y", "$.getBlockY()", PlaceholderType.NUMBER);
        ph.property(RefTypes.LOCATION, "z", "$.getBlockZ()", PlaceholderType.NUMBER);
        ph.property(RefTypes.LOCATION, "world", "$.getWorld().getName()");
        ph.defaultProperty(RefTypes.LOCATION, "world");
    }
}
