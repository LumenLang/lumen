package dev.lumenlang.lumen.plugin.defaults.expression.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Phantom;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class PhantomExpressions {

    private static final String FQCN = Phantom.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_ph")
                .intGetter("[get] %e:ENTITY% phantom size",
                        "getSize()",
                        "Returns the phantom's size.",
                        "var sz = mob's phantom size");
    }
}
