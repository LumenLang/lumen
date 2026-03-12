package dev.lumenlang.lumen.plugin.defaults.expression.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
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
