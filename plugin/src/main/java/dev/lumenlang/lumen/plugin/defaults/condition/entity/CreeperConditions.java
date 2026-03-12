package dev.lumenlang.lumen.plugin.defaults.condition.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
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
