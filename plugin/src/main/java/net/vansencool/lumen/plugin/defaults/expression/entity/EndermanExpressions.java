package net.vansencool.lumen.plugin.defaults.expression.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.codegen.EnvironmentAccess.VarHandle;
import net.vansencool.lumen.api.handler.ExpressionHandler.ExpressionResult;
import net.vansencool.lumen.plugin.util.EntityHelper;
import net.vansencool.lumen.plugin.util.EntityValidation;
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
                        "var block = mob carried block",
                        ctx -> {
                            VarHandle h = (VarHandle) ctx.value("e");
                            String java = ctx.java("e");
                            EntityValidation.requireSubtype(h, FQCN, "get carried block");
                            ctx.codegen().addImport(FQCN);
                            return new ExpressionResult(
                                    "(" + java + " instanceof Enderman _en && _en.getCarriedBlock() != null"
                                            + " ? _en.getCarriedBlock().getMaterial().name() : null)",
                                    null);
                        });
    }
}
