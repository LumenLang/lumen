package dev.lumenlang.lumen.plugin.defaults.statement.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Ageable;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class AgeableStatements {

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
                        "set mob's age lock to true");
    }
}
