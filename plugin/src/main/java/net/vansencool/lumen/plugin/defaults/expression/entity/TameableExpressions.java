package net.vansencool.lumen.plugin.defaults.expression.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.codegen.EnvironmentAccess.VarHandle;
import net.vansencool.lumen.api.handler.ExpressionHandler.ExpressionResult;
import net.vansencool.lumen.api.type.Types;
import net.vansencool.lumen.plugin.util.EntityHelper;
import net.vansencool.lumen.plugin.util.EntityValidation;
import org.bukkit.entity.Tameable;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class TameableExpressions {

    private static final String TAMEABLE = Tameable.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(TAMEABLE)
                .alias("_tm")
                .expression(
                        "[get] %e:ENTITY% owner",
                        "Returns the tameable entity's owner, or null if not tamed.",
                        "var owner = mob owner",
                        ctx -> {
                            VarHandle h = (VarHandle) ctx.value("e");
                            String java = ctx.java("e");
                            EntityValidation.requireSubtype(h, TAMEABLE, "get owner");
                            ctx.codegen().addImport(TAMEABLE);
                            return new ExpressionResult(
                                    "(" + java + " instanceof Tameable _tm && _tm.getOwner() != null"
                                            + " ? _tm.getOwner() : null)",
                                    Types.PLAYER.id());
                        });
    }
}
