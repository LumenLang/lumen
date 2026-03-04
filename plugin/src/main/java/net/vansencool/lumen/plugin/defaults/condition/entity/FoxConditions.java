package net.vansencool.lumen.plugin.defaults.condition.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
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
        "%e:ENTITY% is sleeping",
        "%e:ENTITY% is not sleeping",
        "isSleeping()",
        "Checks if a fox is sleeping.",
        "Checks if a fox is not sleeping.",
        "if mob is sleeping:",
        "if mob is not sleeping:")
        .conditionPair(
        "%e:ENTITY% is crouching",
        "%e:ENTITY% is not crouching",
        "isCrouching()",
        "Checks if a fox is crouching.",
        "Checks if a fox is not crouching.",
        "if mob is crouching:",
        "if mob is not crouching:");
    }
}
