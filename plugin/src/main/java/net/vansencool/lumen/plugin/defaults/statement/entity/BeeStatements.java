package net.vansencool.lumen.plugin.defaults.statement.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Bee;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class BeeStatements {

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
                        "set mob's anger to 400");
    }
}
