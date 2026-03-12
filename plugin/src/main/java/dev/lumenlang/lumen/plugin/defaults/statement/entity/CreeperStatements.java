package dev.lumenlang.lumen.plugin.defaults.statement.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Creeper;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class CreeperStatements {

    private static final String FQCN = Creeper.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_cr")
                .boolSetter("set %e:ENTITY% (powered|charged) [to] %val:BOOLEAN%",
                        "setPowered",
                        "Sets whether a creeper is powered (charged).",
                        "set mob's powered to true")
                .intSetter("set %e:ENTITY% explosion radius [to] %val:INT%",
                        "setExplosionRadius",
                        "Sets the explosion radius of a creeper.",
                        "set mob's explosion radius to 5")
                .intSetter("set %e:ENTITY% max fuse ticks [to] %val:INT%",
                        "setMaxFuseTicks",
                        "Sets a creeper's maximum fuse ticks before detonation.",
                        "set mob's max fuse ticks to 40")
                .action("ignite %e:ENTITY%",
                        "ignite()",
                        "Ignites a creeper, starting its fuse countdown.",
                        "ignite mob")
                .action("explode %e:ENTITY%",
                        "explode()",
                        "Immediately detonates a creeper.",
                        "explode mob");
    }
}
