package net.vansencool.lumen.plugin.defaults.expression.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Slime;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class SlimeExpressions {

    private static final String FQCN = Slime.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_sl")
                .intGetter("[get] %e:ENTITY% slime size",
                        "getSize()",
                        "Returns the slime's size.",
                        "var sz = mob's slime size");
    }
}
