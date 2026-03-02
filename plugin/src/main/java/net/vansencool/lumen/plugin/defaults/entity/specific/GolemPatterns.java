package net.vansencool.lumen.plugin.defaults.entity.specific;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.IronGolem;
import org.jetbrains.annotations.NotNull;

/**
 * Registers iron golem-specific statement and condition patterns.
 *
 * @see EntityHelper
 */
@Registration
@Description("Registers iron golem patterns: player created.")
@SuppressWarnings("unused")
public final class GolemPatterns {

    private static final String FQCN = IronGolem.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_ig")
                .boolSetter("set %e:ENTITY% player created [to] %val:BOOLEAN%",
                        "setPlayerCreated",
                        "Sets whether an iron golem was created by a player.",
                        "set mob's player created to true")
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
