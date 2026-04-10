package dev.lumenlang.lumen.plugin.defaults.condition.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Bee;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class BeeConditions {

    private static final String FQCN = Bee.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_be")
                .conditionPair(
                        "%e:ENTITY% (has|does not have|has no) nectar",
                        0, "has",
                        "hasNectar()",
                        "Checks if a bee has or does not have nectar.",
                        "if mob has nectar:")
                .conditionPair(
                        "%e:ENTITY% (has|has not) stung",
                        0, "has",
                        "hasStung()",
                        "Checks if a bee has or has not stung something.",
                        "if mob has stung:");
    }
}
