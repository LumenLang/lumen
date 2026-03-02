package net.vansencool.lumen.plugin.defaults.entity.specific;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Wolf;
import org.jetbrains.annotations.NotNull;

/**
 * Registers wolf-specific statement, condition, and expression patterns.
 *
 * @see EntityHelper
 */
@Registration
@Description("Registers wolf patterns: angry, collar color.")
@SuppressWarnings("unused")
public final class WolfPatterns {

    private static final String FQCN = Wolf.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_wf")
                .boolSetter("set %e:ENTITY% angry [to] %val:BOOLEAN%",
                        "setAngry",
                        "Sets whether a wolf is angry.",
                        "set mob's angry to true")
                .typedEnumSetter("set %e:ENTITY% collar color [to] %color:DYE_COLOR%",
                        "setCollarColor", "color",
                        "Sets a wolf's collar color.",
                        "set mob's collar color to red")
                .conditionPair(
                        "%e:ENTITY% is angry",
                        "%e:ENTITY% is not angry",
                        "isAngry()",
                        "Checks if a wolf is angry.",
                        "Checks if a wolf is not angry.",
                        "if mob is angry:",
                        "if mob is not angry:")
                .stringGetter("[get] %e:ENTITY% collar color",
                        "getCollarColor().name()",
                        "Returns the wolf's collar color name.",
                        "var c = mob's collar color");
    }
}
