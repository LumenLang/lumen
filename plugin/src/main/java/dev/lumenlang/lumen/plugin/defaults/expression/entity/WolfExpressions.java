package dev.lumenlang.lumen.plugin.defaults.expression.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Wolf;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class WolfExpressions {

    private static final String FQCN = Wolf.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_wf")
                .stringGetter("[get] %e:ENTITY% collar color",
                        "getCollarColor().name()",
                        "Returns the wolf's collar color name.",
                        "var c = mob's collar color");
    }
}
