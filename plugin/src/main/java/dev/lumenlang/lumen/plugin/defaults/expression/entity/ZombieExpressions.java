package dev.lumenlang.lumen.plugin.defaults.expression.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Zombie;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class ZombieExpressions {

    private static final String FQCN = Zombie.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_zb")
                .boolGetter("[is] %e:ENTITY% baby zombie",
                        "isBaby()",
                        "Returns whether the zombie is a baby.",
                        "var isBaby = is mob's baby zombie");
    }
}
