package net.vansencool.lumen.plugin.defaults.expression.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
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
        "var prof = mob's profession")
        .intGetter("[get] %e:ENTITY% villager level",
        "getVillagerLevel()",
        "Returns the villager's trading level.",
        "var lvl = mob's villager level")
        .stringGetter("[get] %e:ENTITY% villager type",
        "getVillagerType().name()",
        "Returns the villager's biome type name.",
        "var vt = mob's villager type");
    }
}
