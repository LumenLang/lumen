package dev.lumenlang.lumen.plugin.defaults.expression.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
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
                        "set a to mob's age");
    }
}
