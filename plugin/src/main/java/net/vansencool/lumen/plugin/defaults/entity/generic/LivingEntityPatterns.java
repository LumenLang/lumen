package net.vansencool.lumen.plugin.defaults.entity.generic;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.codegen.EnvironmentAccess.VarHandle;
import net.vansencool.lumen.api.handler.ExpressionHandler.ExpressionResult;
import net.vansencool.lumen.api.pattern.Categories;
import net.vansencool.lumen.plugin.defaults.attributes.AttributeNames;
import net.vansencool.lumen.plugin.util.EntityValidation;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Registers statements, conditions, and expressions that require a {@code LivingEntity}.
 *
 * <p>Since {@code LivingEntity} is not a concrete mob type, these patterns use
 * {@link EntityValidation#requireLivingEntity} and generate safe {@code instanceof}
 * guards when the compile-time type cannot be confirmed.
 *
 * <h2>Statements</h2>
 * <ul>
 *   <li>{@code kill/slay <entity>}</li>
 *   <li>{@code set <entity> health to <int>}</li>
 *   <li>{@code damage <entity> by <int>}</li>
 *   <li>{@code heal/restore <entity>}</li>
 * </ul>
 *
 * <h2>Conditions</h2>
 * <ul>
 *   <li>{@code <entity> is dead / alive}</li>
 *   <li>{@code <entity> health >= N}</li>
 * </ul>
 *
 * <h2>Expressions</h2>
 * <ul>
 *   <li>{@code <entity> health / max health}</li>
 * </ul>
 */
@Registration
@Description("Registers LivingEntity patterns: kill, health, damage, heal, alive/dead checks, health expressions.")
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class LivingEntityPatterns {

    private static final String LIVING_ENTITY = LivingEntity.class.getName();
    private static final String ATTRIBUTE = Attribute.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        registerStatements(api);
        registerConditions(api);
        registerExpressions(api);
    }

    private void registerStatements(@NotNull LumenAPI api) {
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

    private void registerConditions(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% is (dead|not alive)")
                .description("Checks if a living entity is dead.")
                .example("if mob is dead:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> {
                    VarHandle h = match.ref("e");
                    boolean known = EntityValidation.requireLivingEntity(h, "is dead");
                    ctx.addImport(LIVING_ENTITY);
                    if (known) return "((LivingEntity) " + h.java() + ").isDead()";
                    return "(" + h.java() + " instanceof LivingEntity _le && _le.isDead())";
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% is alive")
                .description("Checks if a living entity is alive.")
                .example("if mob is alive:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> {
                    VarHandle h = match.ref("e");
                    boolean known = EntityValidation.requireLivingEntity(h, "is alive");
                    ctx.addImport(LIVING_ENTITY);
                    if (known) return "!((LivingEntity) " + h.java() + ").isDead()";
                    return "(" + h.java() + " instanceof LivingEntity _le && !_le.isDead())";
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY_POSSESSIVE% health %op:OP% %n:INT%")
                .description("Checks if a living entity's health satisfies a comparison.")
                .example("if mob's health >= 10:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((match, env, ctx) -> {
                    VarHandle h = match.ref("e");
                    boolean known = EntityValidation.requireLivingEntity(h, "health check");
                    ctx.addImport(LIVING_ENTITY);
                    String op = match.java("op", ctx, env);
                    String n = match.java("n", ctx, env);
                    if (known) return "((LivingEntity) " + h.java() + ").getHealth() " + op + " " + n;
                    return "(" + h.java() + " instanceof LivingEntity _le && _le.getHealth() " + op + " " + n + ")";
                }));
    }

    private void registerExpressions(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% health")
                .description("Returns the entity's current health (requires LivingEntity).")
                .example("var hp = mob's health")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    VarHandle h = (VarHandle) ctx.value("e");
                    String java = ctx.java("e");
                    boolean known = EntityValidation.requireLivingEntity(h, "get health");
                    ctx.codegen().addImport(LIVING_ENTITY);
                    if (known) {
                        return new ExpressionResult("((LivingEntity) " + java + ").getHealth()", null);
                    }
                    return new ExpressionResult(
                            "(" + java + " instanceof LivingEntity _le ? _le.getHealth() : 0.0)",
                            null);
                }));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %e:ENTITY_POSSESSIVE% max health")
                .description("Returns the entity's max health (requires LivingEntity).")
                .example("var maxHp = mob's max health")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    VarHandle h = (VarHandle) ctx.value("e");
                    String java = ctx.java("e");
                    boolean known = EntityValidation.requireLivingEntity(h, "get max health");
                    String attrName = AttributeNames.resolve("max_health");
                    ctx.codegen().addImport(LIVING_ENTITY);
                    ctx.codegen().addImport(ATTRIBUTE);
                    if (known) {
                        return new ExpressionResult(
                                "((LivingEntity) " + java + ").getAttribute(Attribute." + attrName + ").getValue()",
                                null);
                    }
                    return new ExpressionResult(
                            "(" + java + " instanceof LivingEntity _le ? _le.getAttribute(Attribute." + attrName + ").getValue() : 0.0)",
                            null);
                }));
    }
}
