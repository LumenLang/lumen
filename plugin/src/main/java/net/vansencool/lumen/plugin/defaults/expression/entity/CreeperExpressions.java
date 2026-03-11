package net.vansencool.lumen.plugin.defaults.expression.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Creeper;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class CreeperExpressions {

    private static final String FQCN = Creeper.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_cr")
                .intGetter("[get] %e:ENTITY% explosion radius",
                        "getExplosionRadius()",
                        "Returns the creeper's explosion radius.",
                        "var r = mob's explosion radius")
                .intGetter("[get] %e:ENTITY% max fuse ticks",
                        "getMaxFuseTicks()",
                        "Returns the creeper's max fuse ticks before detonation.",
                        "var fuse = mob's max fuse ticks");
    }
}
