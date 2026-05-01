package dev.lumenlang.lumen.plugin.defaults.statement.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.TypeEnv.VarHandle;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
import dev.lumenlang.lumen.plugin.util.EntityValidation;
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
                        ctx -> {
                            VarHandle h = (VarHandle) ctx.value("e");
                            EntityValidation.requireSubtype(h, FQCN, "set carried block");
                            ctx.codegen().addImport(FQCN);
                            ctx.out().line("if (" + ctx.java("e")
                                    + " instanceof Enderman _en) { _en.setCarriedMaterial("
                                    + ctx.java("mat") + ".createBlockData()); }");
                        })
                .action("clear %e:ENTITY% carried block",
                        "setCarriedBlock(null)",
                        "Clears the block an enderman is carrying.",
                        "clear mob carried block");
    }
}
