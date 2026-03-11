package net.vansencool.lumen.plugin.defaults.statement.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.codegen.EnvironmentAccess.VarHandle;
import net.vansencool.lumen.plugin.util.EntityHelper;
import net.vansencool.lumen.plugin.util.EntityValidation;
import org.bukkit.entity.Enderman;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class EndermanStatements {

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
                        "clear mob carried block");
    }
}
