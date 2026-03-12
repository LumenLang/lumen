package dev.lumenlang.lumen.plugin.defaults.condition.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class TameableConditions {

    private static final String TAMEABLE = Tameable.class.getName();
    private static final String SITTABLE = Sittable.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(TAMEABLE)
                .alias("_tm")
                .conditionPair(
                        "%e:ENTITY% is tamed",
                        "%e:ENTITY% is not tamed",
                        "isTamed()",
                        "Checks if a tameable entity is tamed.",
                        "Checks if a tameable entity is not tamed.",
                        "if mob is tamed:",
                        "if mob is not tamed:");

        EntityHelper.forType(SITTABLE)
                .alias("_st")
                .conditionPair(
                        "%e:ENTITY% is sitting",
                        "%e:ENTITY% is not sitting",
                        "isSitting()",
                        "Checks if a sittable entity (wolf, cat, etc.) is sitting.",
                        "Checks if a sittable entity (wolf, cat, etc.) is not sitting.",
                        "if mob is sitting:",
                        "if mob is not sitting:");
    }
}
