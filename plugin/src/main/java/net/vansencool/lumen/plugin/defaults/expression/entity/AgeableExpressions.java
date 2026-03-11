package net.vansencool.lumen.plugin.defaults.expression.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Ageable;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class AgeableExpressions {

    private static final String FQCN = Ageable.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_ag")
        .intGetter("[get] %e:ENTITY% age",
        "getAge()",
        "Returns the ageable entity's age value.",
        "var a = mob's age");
    }
}
