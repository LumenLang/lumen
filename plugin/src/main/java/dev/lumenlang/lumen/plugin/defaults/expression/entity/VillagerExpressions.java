package dev.lumenlang.lumen.plugin.defaults.expression.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Villager;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class VillagerExpressions {

    private static final String FQCN = Villager.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_vl")
                .stringGetter("[get] %e:ENTITY% profession",
                        "getProfession().name()",
                        "Returns the villager's profession name.",
                        "set prof to mob's profession")
                .intGetter("[get] %e:ENTITY% villager level",
                        "getVillagerLevel()",
                        "Returns the villager's trading level.",
                        "set lvl to mob's villager level")
                .stringGetter("[get] %e:ENTITY% villager type",
                        "getVillagerType().name()",
                        "Returns the villager's biome type name.",
                        "set vt to mob's villager type");
    }
}
