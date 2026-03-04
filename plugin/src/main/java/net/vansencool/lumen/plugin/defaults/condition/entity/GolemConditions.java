package net.vansencool.lumen.plugin.defaults.condition.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.IronGolem;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class GolemConditions {

    private static final String FQCN = IronGolem.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_ig")
        .conditionPair(
        "%e:ENTITY% is player created",
        "%e:ENTITY% is not player created",
        "isPlayerCreated()",
        "Checks if an iron golem was created by a player.",
        "Checks if an iron golem was not created by a player.",
        "if mob is player created:",
        "if mob is not player created:");
    }
}
