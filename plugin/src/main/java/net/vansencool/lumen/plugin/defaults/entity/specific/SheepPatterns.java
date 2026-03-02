package net.vansencool.lumen.plugin.defaults.entity.specific;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Sheep;
import org.jetbrains.annotations.NotNull;

/**
 * Registers sheep-specific statement, condition, and expression patterns.
 *
 * @see EntityHelper
 */
@Registration
@Description("Registers sheep patterns: shear, sheared, wool color.")
@SuppressWarnings("unused")
public final class SheepPatterns {

    private static final String FQCN = Sheep.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_sh")
                .action("shear %e:ENTITY%",
                        "setSheared(true)",
                        "Shears a sheep.",
                        "shear mob")
                .boolSetter("set %e:ENTITY% sheared [to] %val:BOOLEAN%",
                        "setSheared",
                        "Sets whether a sheep is sheared.",
                        "set mob's sheared to true")
                .typedEnumSetter("set %e:ENTITY% (wool|sheep) color [to] %color:DYE_COLOR%",
                        "setColor", "color",
                        "Sets a sheep's wool color.",
                        "set mob's wool color to red")
                .conditionPair(
                        "%e:ENTITY% is sheared",
                        "%e:ENTITY% is not sheared",
                        "isSheared()",
                        "Checks if a sheep is sheared.",
                        "Checks if a sheep is not sheared.",
                        "if mob is sheared:",
                        "if mob is not sheared:")
                .stringGetter("[get] %e:ENTITY% (wool|sheep) color",
                        "getColor().name()",
                        "Returns the sheep's wool color name.",
                        "var c = mob's sheep color");
    }
}
