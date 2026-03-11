package net.vansencool.lumen.plugin.defaults.condition.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.codegen.EnvironmentAccess.VarHandle;
import net.vansencool.lumen.plugin.util.EntityHelper;
import net.vansencool.lumen.plugin.util.EntityValidation;
import org.bukkit.entity.Slime;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class SlimeConditions {

    private static final String FQCN = Slime.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_sl")
                .condition(
                        "%e:ENTITY% size %op:OP% %n:INT%",
                        "Checks if a slime's size satisfies a comparison.",
                        "if mob size >= 2:",
                        (match, env, ctx) -> {
                            VarHandle h = match.ref("e");
                            EntityValidation.requireSubtype(h, FQCN, "slime size");
                            ctx.addImport(FQCN);
                            return "(" + h.java()
                                    + " instanceof Slime _sl && _sl.getSize() "
                                    + match.java("op", ctx, env) + " "
                                    + match.java("n", ctx, env) + ")";
                        });
    }
}
