package net.vansencool.lumen.plugin.defaults.condition.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Ageable;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class AgeableConditions {

    private static final String FQCN = Ageable.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_ag")
                .conditionPair(
                        "%e:ENTITY% is (adult|grown)",
                        "%e:ENTITY% is (baby|young)",
                        "isAdult()",
                        "Checks if an ageable entity is an adult.",
                        "Checks if an ageable entity is a baby.",
                        "if mob is adult:",
                        "if mob is baby:");
    }
}
