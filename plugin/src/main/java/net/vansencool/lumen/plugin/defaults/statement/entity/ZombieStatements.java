package net.vansencool.lumen.plugin.defaults.statement.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Zombie;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class ZombieStatements {

    private static final String FQCN = Zombie.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_zb")
        .boolSetter("set %e:ENTITY% can break doors [to] %val:BOOLEAN%",
        "setCanBreakDoors",
        "Sets whether a zombie can break doors.",
        "set mob's can break doors to true")
        .boolSetter("set %e:ENTITY% (baby|young) [to] %val:BOOLEAN%",
        "setBaby",
        "Sets whether a zombie is a baby.",
        "set mob's baby to true");
    }
}
