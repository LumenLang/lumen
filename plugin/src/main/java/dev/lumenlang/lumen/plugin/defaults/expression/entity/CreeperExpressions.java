package dev.lumenlang.lumen.plugin.defaults.expression.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
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
                        "set r to mob's explosion radius")
                .intGetter("[get] %e:ENTITY% max fuse ticks",
                        "getMaxFuseTicks()",
                        "Returns the creeper's max fuse ticks before detonation.",
                        "set fuse to mob's max fuse ticks");
    }
}
