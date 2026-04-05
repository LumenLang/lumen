package dev.lumenlang.lumen.plugin.defaults.expression.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Bee;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class BeeExpressions {

    private static final String FQCN = Bee.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_be")
                .intGetter("[get] %e:ENTITY% anger [level]",
                        "getAnger()",
                        "Returns the bee's anger level in ticks.",
                        "set anger to mob's anger level");
    }
}
