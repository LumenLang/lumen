package dev.lumenlang.lumen.plugin.defaults.expression.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Sheep;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class SheepExpressions {

    private static final String FQCN = Sheep.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_sh")
                .stringGetter("[get] %e:ENTITY% (wool|sheep) color",
                        "getColor().name()",
                        "Returns the sheep's wool color name.",
                        "var c = mob's sheep color");
    }
}
