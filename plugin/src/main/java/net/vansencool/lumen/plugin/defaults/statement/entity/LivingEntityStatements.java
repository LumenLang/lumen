package net.vansencool.lumen.plugin.defaults.statement.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.codegen.EnvironmentAccess.VarHandle;
import net.vansencool.lumen.api.pattern.Categories;
import net.vansencool.lumen.plugin.defaults.util.AttributeNames;
import net.vansencool.lumen.plugin.util.EntityValidation;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Registers statement patterns that require a {@code LivingEntity}.
 */
@Registration
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class LivingEntityStatements {

    private static final String LIVING_ENTITY = LivingEntity.class.getName();
    private static final String ATTRIBUTE = Attribute.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen").pattern("(kill|slay) %e:ENTITY%")
                .description("Kills a living entity by setting its health to zero.")
                .example("kill mob")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((line, ctx, out) -> {
                    VarHandle h = (VarHandle) ctx.value("e");
                    boolean known = EntityValidation.requireLivingEntity(h, "kill");
                    ctx.codegen().addImport(LIVING_ENTITY);
                    if (known) {
                        out.line("((LivingEntity) " + ctx.java("e") + ").setHealth(0);");
                    } else {
                        out.line("if (" + ctx.java("e") + " instanceof LivingEntity _le) { _le.setHealth(0); }");
                    }
                }));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("set %e:ENTITY_POSSESSIVE% health [to] %val:INT%")
                .description("Sets a living entity's health to the specified value.")
                .example("set mob's health to 10")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((line, ctx, out) -> {
                    VarHandle h = (VarHandle) ctx.value("e");
                    boolean known = EntityValidation.requireLivingEntity(h, "set health");
                    ctx.codegen().addImport(LIVING_ENTITY);
                    if (known) {
                        out.line("((LivingEntity) " + ctx.java("e") + ").setHealth(" + ctx.java("val") + ");");
                    } else {
                        out.line("if (" + ctx.java("e") + " instanceof LivingEntity _le) { _le.setHealth(" + ctx.java("val") + "); }");
                    }
                }));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("damage %e:ENTITY% [by] %val:INT%")
                .description("Deals damage to a living entity.")
                .example("damage mob by 5")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((line, ctx, out) -> {
                    VarHandle h = (VarHandle) ctx.value("e");
                    boolean known = EntityValidation.requireLivingEntity(h, "damage");
                    ctx.codegen().addImport(LIVING_ENTITY);
                    if (known) {
                        out.line("((LivingEntity) " + ctx.java("e") + ").damage(" + ctx.java("val") + ");");
                    } else {
                        out.line("if (" + ctx.java("e") + " instanceof LivingEntity _le) { _le.damage(" + ctx.java("val") + "); }");
                    }
                }));

        api.patterns().statement(b -> b
                .by("Lumen").pattern("(heal|restore) %e:ENTITY%")
                .description("Fully heals a living entity to its max health.")
                .example("heal mob")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((line, ctx, out) -> {
                    VarHandle h = (VarHandle) ctx.value("e");
                    boolean known = EntityValidation.requireLivingEntity(h, "heal");
                    String attrName = AttributeNames.resolve("max_health");
                    ctx.codegen().addImport(LIVING_ENTITY);
                    ctx.codegen().addImport(ATTRIBUTE);
                    if (known) {
                        out.line("((LivingEntity) " + ctx.java("e") + ").setHealth(((LivingEntity) " + ctx.java("e") + ").getAttribute(Attribute." + attrName + ").getValue());");
                    } else {
                        out.line("if (" + ctx.java("e") + " instanceof LivingEntity _le) { _le.setHealth(_le.getAttribute(Attribute." + attrName + ").getValue()); }");
                    }
                }));
    }
}
