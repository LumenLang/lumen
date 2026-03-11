package net.vansencool.lumen.plugin.defaults.expression.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
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
