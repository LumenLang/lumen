package net.vansencool.lumen.plugin.defaults.entity.specific;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Axolotl;
import org.jetbrains.annotations.NotNull;

/**
 * Registers axolotl-specific statement, condition, and expression patterns.
 *
 * @see EntityHelper
 */
@Registration
@Description("Registers axolotl patterns: playing dead, variant.")
@SuppressWarnings("unused")
public final class AxolotlPatterns {

    private static final String FQCN = Axolotl.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_ax")
                .boolSetter("set %e:ENTITY% playing dead [to] %val:BOOLEAN%",
                        "setPlayingDead",
                        "Sets whether an axolotl is playing dead.",
                        "set mob's playing dead to true")
                .typedEnumSetter("set %e:ENTITY% axolotl variant [to] %variant:AXOLOTL_VARIANT%",
                        "setVariant", "variant",
                        "Sets an axolotl's color variant.",
                        "set mob's axolotl variant to blue")
                .conditionPair(
                        "%e:ENTITY% is playing dead",
                        "%e:ENTITY% is not playing dead",
                        "isPlayingDead()",
                        "Checks if an axolotl is playing dead.",
                        "Checks if an axolotl is not playing dead.",
                        "if mob is playing dead:",
                        "if mob is not playing dead:")
                .stringGetter("[get] %e:ENTITY% axolotl variant",
                        "getVariant().name()",
                        "Returns the axolotl's variant name.",
                        "var v = mob's axolotl variant");
    }
}
