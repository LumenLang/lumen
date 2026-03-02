package net.vansencool.lumen.plugin.defaults.entity.specific;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Bee;
import org.jetbrains.annotations.NotNull;

/**
 * Registers bee-specific statement, condition, and expression patterns.
 *
 * @see EntityHelper
 */
@Registration
@Description("Registers bee patterns: nectar, stung, anger.")
@SuppressWarnings("unused")
public final class BeePatterns {

    private static final String FQCN = Bee.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_be")
                .boolSetter("set %e:ENTITY% has nectar [to] %val:BOOLEAN%",
                        "setHasNectar",
                        "Sets whether a bee has nectar.",
                        "set mob's has nectar to true")
                .boolSetter("set %e:ENTITY% has stung [to] %val:BOOLEAN%",
                        "setHasStung",
                        "Sets whether a bee has stung.",
                        "set mob's has stung to true")
                .intSetter("set %e:ENTITY% anger [to] %val:INT%",
                        "setAnger",
                        "Sets a bee's anger level in ticks.",
                        "set mob's anger to 400")
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
                        "if mob has not stung:")
                .intGetter("[get] %e:ENTITY% anger [level]",
                        "getAnger()",
                        "Returns the bee's anger level in ticks.",
                        "var anger = mob's anger level");
    }
}
