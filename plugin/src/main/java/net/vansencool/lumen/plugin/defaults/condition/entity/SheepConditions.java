package net.vansencool.lumen.plugin.defaults.condition.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Sheep;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class SheepConditions {

    private static final String FQCN = Sheep.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_sh")
        .conditionPair(
        "%e:ENTITY% is sheared",
        "%e:ENTITY% is not sheared",
        "isSheared()",
        "Checks if a sheep is sheared.",
        "Checks if a sheep is not sheared.",
        "if mob is sheared:",
        "if mob is not sheared:");
    }
}
