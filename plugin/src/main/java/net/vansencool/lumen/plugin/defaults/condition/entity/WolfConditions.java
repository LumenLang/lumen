package net.vansencool.lumen.plugin.defaults.condition.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Wolf;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class WolfConditions {

    private static final String FQCN = Wolf.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_wf")
        .conditionPair(
        "%e:ENTITY% is angry",
        "%e:ENTITY% is not angry",
        "isAngry()",
        "Checks if a wolf is angry.",
        "Checks if a wolf is not angry.",
        "if mob is angry:",
        "if mob is not angry:");
    }
}
