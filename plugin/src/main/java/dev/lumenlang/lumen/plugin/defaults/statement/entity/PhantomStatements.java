package dev.lumenlang.lumen.plugin.defaults.statement.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Phantom;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class PhantomStatements {

    private static final String FQCN = Phantom.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_ph")
                .intSetter("set %e:ENTITY% phantom size [to] %val:INT%",
                        "setSize",
                        "Sets a phantom's size.",
                        "set mob's phantom size to 3");
    }
}
