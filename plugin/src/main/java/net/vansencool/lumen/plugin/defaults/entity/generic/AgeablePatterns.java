package net.vansencool.lumen.plugin.defaults.entity.generic;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Ageable;
import org.jetbrains.annotations.NotNull;

/**
 * Registers ageable entity patterns (baby, adult, age, age lock).
 *
 * @see EntityHelper
 */
@Registration
@Description("Registers ageable patterns: baby, adult, age, age lock.")
@SuppressWarnings("unused")
public final class AgeablePatterns {

    private static final String FQCN = Ageable.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_ag")
                .action("set %e:ENTITY% (baby|young)",
                        "setBaby()",
                        "Makes an ageable entity a baby.",
                        "set mob baby")
                .action("set %e:ENTITY% (adult|grown)",
                        "setAdult()",
                        "Makes an ageable entity an adult.",
                        "set mob adult")
                .intSetter("set %e:ENTITY% age [to] %val:INT%",
                        "setAge",
                        "Sets the age of an ageable entity in ticks.",
                        "set mob's age to -24000")
                .boolSetter("set %e:ENTITY% age lock [to] %val:BOOLEAN%",
                        "setAgeLock",
                        "Locks or unlocks an ageable entity's age.",
                        "set mob's age lock to true")
                .conditionPair(
                        "%e:ENTITY% is (adult|grown)",
                        "%e:ENTITY% is (baby|young)",
                        "isAdult()",
                        "Checks if an ageable entity is an adult.",
                        "Checks if an ageable entity is a baby.",
                        "if mob is adult:",
                        "if mob is baby:")
                .intGetter("[get] %e:ENTITY% age",
                        "getAge()",
                        "Returns the ageable entity's age value.",
                        "var a = mob's age");
    }
}
