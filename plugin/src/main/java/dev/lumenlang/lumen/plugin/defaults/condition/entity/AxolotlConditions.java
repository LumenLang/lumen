package dev.lumenlang.lumen.plugin.defaults.condition.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Axolotl;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class AxolotlConditions {

    private static final String FQCN = Axolotl.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_ax")
                .conditionPair(
                        "%e:ENTITY% (is|is not) playing dead",
                        0, "is",
                        "isPlayingDead()",
                        "Checks if an axolotl is or is not playing dead.",
                        "if mob is playing dead:");
    }
}
