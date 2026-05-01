package dev.lumenlang.lumen.plugin.defaults.expression.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.TypeEnv.VarHandle;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
import dev.lumenlang.lumen.plugin.util.EntityValidation;
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
                        "set owner to mob owner",
                        ctx -> {
                            VarHandle h = (VarHandle) ctx.value("e");
                            String java = ctx.java("e");
                            EntityValidation.requireSubtype(h, TAMEABLE, "get owner");
                            ctx.codegen().addImport(TAMEABLE);
                            return new ExpressionResult(
                                    "(" + java + " instanceof Tameable _tm && _tm.getOwner() != null"
                                            + " ? _tm.getOwner() : null)",
                                    MinecraftTypes.OFFLINE_PLAYER);
                        });
    }
}
