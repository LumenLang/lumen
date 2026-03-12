package dev.lumenlang.lumen.plugin.defaults.condition.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Pig;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class PigConditions {

    private static final String FQCN = Pig.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_pg")
                .conditionPair(
                        "%e:ENTITY% has saddle",
                        "%e:ENTITY% (does not have|has no) saddle",
                        "hasSaddle()",
                        "Checks if a pig has a saddle.",
                        "Checks if a pig does not have a saddle.",
                        "if mob has saddle:",
                        "if mob has no saddle:");
    }
}
