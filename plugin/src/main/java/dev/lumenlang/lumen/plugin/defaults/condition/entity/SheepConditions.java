package dev.lumenlang.lumen.plugin.defaults.condition.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
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
