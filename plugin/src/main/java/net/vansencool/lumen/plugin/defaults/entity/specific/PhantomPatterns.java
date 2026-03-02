package net.vansencool.lumen.plugin.defaults.entity.specific;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Phantom;
import org.jetbrains.annotations.NotNull;

/**
 * Registers phantom-specific statement and expression patterns.
 *
 * @see EntityHelper
 */
@Registration
@Description("Registers phantom patterns: size.")
@SuppressWarnings("unused")
public final class PhantomPatterns {

    private static final String FQCN = Phantom.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_ph")
                .intSetter("set %e:ENTITY% phantom size [to] %val:INT%",
                        "setSize",
                        "Sets a phantom's size.",
                        "set mob's phantom size to 3")
                .intGetter("[get] %e:ENTITY% phantom size",
                        "getSize()",
                        "Returns the phantom's size.",
                        "var sz = mob's phantom size");
    }
}
