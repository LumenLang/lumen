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
                        "%e:ENTITY% has nectar",
                        "%e:ENTITY% (does not have|has no) nectar",
                        "hasNectar()",
                        "Checks if a bee has nectar.",
                        "Checks if a bee does not have nectar.",
                        "if mob has nectar:",
                        "if mob has no nectar:")
                .conditionPair(
                        "%e:ENTITY% has stung",
                        "%e:ENTITY% has not stung",
                        "hasStung()",
                        "Checks if a bee has stung something.",
                        "Checks if a bee has not stung anything.",
                        "if mob has stung:",
                        "if mob has not stung:");
    }
}
