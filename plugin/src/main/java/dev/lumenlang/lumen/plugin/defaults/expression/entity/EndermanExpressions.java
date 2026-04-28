package dev.lumenlang.lumen.plugin.defaults.expression.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess.VarHandle;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
import dev.lumenlang.lumen.plugin.util.EntityValidation;
import org.bukkit.entity.Enderman;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class EndermanExpressions {

    private static final String FQCN = Enderman.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_en")
                .expression(
                        "[get] %e:ENTITY% carried block",
                        "Returns the name of the block the enderman is carrying, or null.",
                        "set block to mob carried block",
                        ctx -> {
                            VarHandle h = (VarHandle) ctx.value("e");
                            String java = ctx.java("e");
                            EntityValidation.requireSubtype(h, FQCN, "get carried block");
                            ctx.codegen().addImport(FQCN);
                            return new ExpressionResult(
                                    "(" + java + " instanceof Enderman _en && _en.getCarriedBlock() != null"
                                            + " ? _en.getCarriedBlock().getMaterial().name() : null)",
                                    PrimitiveType.STRING);
                        });
    }
}
