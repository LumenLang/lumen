package net.vansencool.lumen.plugin.defaults.statement.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Pig;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class PigStatements {

    private static final String FQCN = Pig.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_pg")
                .boolSetter("set %e:ENTITY% saddle [to] %val:BOOLEAN%",
                        "setSaddle",
                        "Sets whether a pig has a saddle.",
                        "set mob's saddle to true");
    }
}
