package net.vansencool.lumen.plugin.defaults.condition.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
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
        "%e:ENTITY% is playing dead",
        "%e:ENTITY% is not playing dead",
        "isPlayingDead()",
        "Checks if an axolotl is playing dead.",
        "Checks if an axolotl is not playing dead.",
        "if mob is playing dead:",
        "if mob is not playing dead:");
    }
}
