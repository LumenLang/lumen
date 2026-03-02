package net.vansencool.lumen.plugin.defaults.entity.generic;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.codegen.EnvironmentAccess.VarHandle;
import net.vansencool.lumen.api.handler.ExpressionHandler.ExpressionResult;
import net.vansencool.lumen.api.type.RefTypes;
import net.vansencool.lumen.plugin.util.EntityHelper;
import net.vansencool.lumen.plugin.util.EntityValidation;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.jetbrains.annotations.NotNull;

/**
 * Registers patterns for tameable and sittable entities.
 *
 * @see EntityHelper
 */
@Registration
@Description("Registers tameable patterns: tame, untame, sitting, owner.")
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class TameablePatterns {

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
                        })
                .conditionPair(
                        "%e:ENTITY% is tamed",
                        "%e:ENTITY% is not tamed",
                        "isTamed()",
                        "Checks if a tameable entity is tamed.",
                        "Checks if a tameable entity is not tamed.",
                        "if mob is tamed:",
                        "if mob is not tamed:")
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
                                    RefTypes.PLAYER.id());
                        });

        EntityHelper.forType(SITTABLE)
                .alias("_st")
                .boolSetter("set %e:ENTITY% sitting [to] %val:BOOLEAN%",
                        "setSitting",
                        "Sets whether a sittable entity is sitting.",
                        "set mob's sitting to true")
                .conditionPair(
                        "%e:ENTITY% is sitting",
                        "%e:ENTITY% is not sitting",
                        "isSitting()",
                        "Checks if a sittable entity (wolf, cat, etc.) is sitting.",
                        "Checks if a sittable entity (wolf, cat, etc.) is not sitting.",
                        "if mob is sitting:",
                        "if mob is not sitting:");
    }
}
