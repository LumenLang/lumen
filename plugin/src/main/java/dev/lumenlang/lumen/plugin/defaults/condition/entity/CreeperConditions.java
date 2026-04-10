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
                        "%e:ENTITY% (is|is not) (powered|charged)",
                        0, "is",
                        "isPowered()",
                        "Checks if a creeper is or is not powered (charged).",
                        "if mob is powered:")
                .conditionPair(
                        "%e:ENTITY% (is|is not) ignited",
                        0, "is",
                        "isIgnited()",
                        "Checks if a creeper is or is not ignited.",
                        "if mob is ignited:");
    }
}
