package net.vansencool.lumen.plugin.defaults.statement.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.EntityHelper;
import org.bukkit.entity.Wolf;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class WolfStatements {

    private static final String FQCN = Wolf.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_wf")
                .boolSetter("set %e:ENTITY% angry [to] %val:BOOLEAN%",
                        "setAngry",
                        "Sets whether a wolf is angry.",
                        "set mob's angry to true")
                .typedEnumSetter("set %e:ENTITY% collar color [to] %color:DYE_COLOR%",
                        "setCollarColor", "color",
                        "Sets a wolf's collar color.",
                        "set mob's collar color to red");
    }
}
