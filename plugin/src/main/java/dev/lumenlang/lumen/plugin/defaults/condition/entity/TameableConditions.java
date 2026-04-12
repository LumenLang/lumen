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
                        "%e:ENTITY% (is|is not) tamed",
                        0, "is",
                        "isTamed()",
                        "Checks if a tameable entity is or is not tamed.",
                        "if mob is tamed:");

        EntityHelper.forType(SITTABLE)
                .alias("_st")
                .conditionPair(
                        "%e:ENTITY% (is|is not) sitting",
                        0, "is",
                        "isSitting()",
                        "Checks if a sittable entity (wolf, cat, etc.) is or is not sitting.",
                        "if mob is sitting:");
    }
}
