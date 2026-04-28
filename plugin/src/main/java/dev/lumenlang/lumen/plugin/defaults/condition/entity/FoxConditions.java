package dev.lumenlang.lumen.plugin.defaults.condition.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Fox;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class FoxConditions {

    private static final String FQCN = Fox.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_fx")
                .conditionPair(
                        "%e:ENTITY% (is|is not) sleeping",
                        0, "is",
                        "isSleeping()",
                        "Checks if a fox is or is not sleeping.",
                        "if mob is sleeping:")
                .conditionPair(
                        "%e:ENTITY% (is|is not) crouching",
                        0, "is",
                        "isCrouching()",
                        "Checks if a fox is or is not crouching.",
                        "if mob is crouching:");
    }
}
