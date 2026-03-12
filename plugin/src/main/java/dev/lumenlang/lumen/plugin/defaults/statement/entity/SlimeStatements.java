package dev.lumenlang.lumen.plugin.defaults.statement.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Slime;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class SlimeStatements {

    private static final String FQCN = Slime.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_sl")
                .intSetter("set %e:ENTITY% slime size [to] %val:INT%",
                        "setSize",
                        "Sets a slime's size.",
                        "set mob's slime size to 3");
    }
}
