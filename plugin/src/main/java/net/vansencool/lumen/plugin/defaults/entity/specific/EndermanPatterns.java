package net.vansencool.lumen.plugin.defaults.entity.specific;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.codegen.EnvironmentAccess.VarHandle;
import net.vansencool.lumen.api.handler.ExpressionHandler.ExpressionResult;
import net.vansencool.lumen.plugin.util.EntityHelper;
import net.vansencool.lumen.plugin.util.EntityValidation;
import org.bukkit.entity.Enderman;
import org.jetbrains.annotations.NotNull;

/**
 * Registers enderman-specific statement and expression patterns.
 *
 * @see EntityHelper
 */
@Registration
@Description("Registers enderman patterns: carried block.")
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class EndermanPatterns {

    private static final String FQCN = Enderman.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(FQCN)
                .alias("_en")
                .statement(
                        "set %e:ENTITY% carried block [to] %mat:MATERIAL%",
                        "Sets the block an enderman is carrying.",
                        "set mob carried block to grass_block",
                        (line, ctx, out) -> {
                            VarHandle h = (VarHandle) ctx.value("e");
                            EntityValidation.requireSubtype(h, FQCN, "set carried block");
                            ctx.codegen().addImport(FQCN);
                            out.line("if (" + ctx.java("e")
                                    + " instanceof Enderman _en) { _en.setCarriedMaterial("
                                    + ctx.java("mat") + ".createBlockData()); }");
                        })
                .action("clear %e:ENTITY% carried block",
                        "setCarriedBlock(null)",
                        "Clears the block an enderman is carrying.",
                        "clear mob carried block")
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
