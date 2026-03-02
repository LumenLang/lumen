package net.vansencool.lumen.plugin.defaults.entity.specific;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Pig;
import org.jetbrains.annotations.NotNull;

/**
 * Registers pig-specific statement and condition patterns.
 *
 * @see EntityHelper
 */
@Registration
@Description("Registers pig patterns: saddle.")
@SuppressWarnings("unused")
public final class PigPatterns {

    private static final String FQCN = Pig.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_pg")
                .boolSetter("set %e:ENTITY% saddle [to] %val:BOOLEAN%",
                        "setSaddle",
                        "Sets whether a pig has a saddle.",
                        "set mob's saddle to true")
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
