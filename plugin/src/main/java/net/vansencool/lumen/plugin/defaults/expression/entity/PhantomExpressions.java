package net.vansencool.lumen.plugin.defaults.expression.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
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
