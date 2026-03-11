package net.vansencool.lumen.plugin.defaults.expression.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
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
        "var anger = mob's anger level");
    }
}
