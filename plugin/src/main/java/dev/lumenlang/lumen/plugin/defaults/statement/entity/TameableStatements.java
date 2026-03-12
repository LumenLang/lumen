package dev.lumenlang.lumen.plugin.defaults.statement.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess.VarHandle;
import dev.lumenlang.lumen.plugin.util.EntityHelper;
import dev.lumenlang.lumen.plugin.util.EntityValidation;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class TameableStatements {

    private static final String TAMEABLE = Tameable.class.getName();
    private static final String SITTABLE = Sittable.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        EntityHelper.forType(TAMEABLE)
                .alias("_tm")
                .statement(
                        "tame %e:ENTITY% [by] %who:PLAYER%",
                        "Tames an entity, assigning it to a player as owner.",
                        "tame mob by player",
                        (line, ctx, out) -> {
                            VarHandle h = (VarHandle) ctx.value("e");
                            EntityValidation.requireSubtype(h, TAMEABLE, "tame");
                            ctx.codegen().addImport(TAMEABLE);
                            out.line("if (" + ctx.java("e")
                                    + " instanceof Tameable _tm) { _tm.setTamed(true); _tm.setOwner("
                                    + ctx.java("who") + "); }");
                        })
                .statement(
                        "untame %e:ENTITY%",
                        "Untames a tameable entity, removing its owner.",
                        "untame mob",
                        (line, ctx, out) -> {
                            VarHandle h = (VarHandle) ctx.value("e");
                            EntityValidation.requireSubtype(h, TAMEABLE, "untame");
                            ctx.codegen().addImport(TAMEABLE);
                            out.line("if (" + ctx.java("e")
                                    + " instanceof Tameable _tm) { _tm.setTamed(false); _tm.setOwner(null); }");
                        });

        EntityHelper.forType(SITTABLE)
                .alias("_st")
                .boolSetter("set %e:ENTITY% sitting [to] %val:BOOLEAN%",
                        "setSitting",
                        "Sets whether a sittable entity is sitting.",
                        "set mob's sitting to true");
    }
}
