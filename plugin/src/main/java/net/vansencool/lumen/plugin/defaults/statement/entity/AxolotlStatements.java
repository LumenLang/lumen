package net.vansencool.lumen.plugin.defaults.statement.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Axolotl;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class AxolotlStatements {

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
        "set mob's axolotl variant to blue");
    }
}
