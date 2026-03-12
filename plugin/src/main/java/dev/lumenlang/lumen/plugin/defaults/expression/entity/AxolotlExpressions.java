package dev.lumenlang.lumen.plugin.defaults.expression.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Axolotl;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class AxolotlExpressions {

    private static final String FQCN = Axolotl.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_ax")
                .stringGetter("[get] %e:ENTITY% axolotl variant",
                        "getVariant().name()",
                        "Returns the axolotl's variant name.",
                        "var v = mob's axolotl variant");
    }
}
