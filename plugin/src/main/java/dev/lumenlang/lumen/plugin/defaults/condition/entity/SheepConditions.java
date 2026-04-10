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
                        "%e:ENTITY% (is|is not) sheared",
                        0, "is",
                        "isSheared()",
                        "Checks if a sheep is or is not sheared.",
                        "if mob is sheared:");
    }
}
