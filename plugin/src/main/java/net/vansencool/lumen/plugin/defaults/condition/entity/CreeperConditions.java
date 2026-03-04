package net.vansencool.lumen.plugin.defaults.condition.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Creeper;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class CreeperConditions {

    private static final String FQCN = Creeper.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_cr")
        .conditionPair(
        "%e:ENTITY% is (powered|charged)",
        "%e:ENTITY% is not (powered|charged)",
        "isPowered()",
        "Checks if a creeper is powered (charged).",
        "Checks if a creeper is not powered (charged).",
        "if mob is powered:",
        "if mob is not powered:")
        .conditionPair(
        "%e:ENTITY% is ignited",
        "%e:ENTITY% is not ignited",
        "isIgnited()",
        "Checks if a creeper has been ignited.",
        "Checks if a creeper has not been ignited.",
        "if mob is ignited:",
        "if mob is not ignited:");
    }
}
