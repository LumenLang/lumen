package net.vansencool.lumen.plugin.defaults.expression.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
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
