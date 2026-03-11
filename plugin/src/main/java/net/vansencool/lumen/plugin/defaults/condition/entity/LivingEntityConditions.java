package net.vansencool.lumen.plugin.defaults.condition.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.codegen.EnvironmentAccess.VarHandle;
import net.vansencool.lumen.api.pattern.Categories;
import net.vansencool.lumen.plugin.util.EntityValidation;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Registers condition patterns that require a {@code LivingEntity}.
 */
@Registration
@SuppressWarnings("unused")
public final class LivingEntityConditions {

    private static final String LIVING_ENTITY = LivingEntity.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
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
}
