package dev.lumenlang.lumen.plugin.defaults.statement.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Sheep;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class SheepStatements {

    private static final String FQCN = Sheep.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_sh")
                .action("shear %e:ENTITY%",
                        "setSheared(true)",
                        "Shears a sheep.",
                        "shear mob")
                .boolSetter("set %e:ENTITY% sheared [to] %val:BOOLEAN%",
                        "setSheared",
                        "Sets whether a sheep is sheared.",
                        "set mob's sheared to true")
                .typedEnumSetter("set %e:ENTITY% (wool|sheep) color [to] %color:DYE_COLOR%",
                        "setColor", "color",
                        "Sets a sheep's wool color.",
                        "set mob's wool color to red");
    }
}
