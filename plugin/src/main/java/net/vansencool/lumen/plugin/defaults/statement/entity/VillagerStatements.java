package net.vansencool.lumen.plugin.defaults.statement.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class VillagerStatements {

    private static final String FQCN = Villager.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_vl")
        .intSetter("set %e:ENTITY% villager level [to] %val:INT%",
        "setVillagerLevel",
        "Sets a villager's trading level.",
        "set mob's villager level to 3");
    }
}
