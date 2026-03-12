package dev.lumenlang.lumen.plugin.defaults.statement.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.IronGolem;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class GolemStatements {

    private static final String FQCN = IronGolem.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_ig")
                .boolSetter("set %e:ENTITY% player created [to] %val:BOOLEAN%",
                        "setPlayerCreated",
                        "Sets whether an iron golem was created by a player.",
                        "set mob's player created to true");
    }
}
