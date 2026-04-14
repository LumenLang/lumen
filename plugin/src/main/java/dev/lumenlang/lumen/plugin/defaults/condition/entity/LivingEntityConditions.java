package dev.lumenlang.lumen.plugin.defaults.condition.entity;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess.VarHandle;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.plugin.util.EntityValidation;
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
                .handler(ctx -> {
                    VarHandle h = ctx.requireVarHandle("e");
                    boolean known = EntityValidation.requireLivingEntity(h, "is dead");
                    ctx.codegen().addImport(LIVING_ENTITY);
                    if (known) return "((LivingEntity) " + h.java() + ").isDead()";
                    return "(" + h.java() + " instanceof LivingEntity _le && _le.isDead())";
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% is alive")
                .description("Checks if a living entity is alive.")
                .example("if mob is alive:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    VarHandle h = ctx.requireVarHandle("e");
                    boolean known = EntityValidation.requireLivingEntity(h, "is alive");
                    ctx.codegen().addImport(LIVING_ENTITY);
                    if (known) return "!((LivingEntity) " + h.java() + ").isDead()";
                    return "(" + h.java() + " instanceof LivingEntity _le && !_le.isDead())";
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY_POSSESSIVE% health %op:OP% %n:INT%")
                .description("Checks if a living entity's health satisfies a comparison.")
                .example("if mob's health >= 10:")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    VarHandle h = ctx.requireVarHandle("e");
                    boolean known = EntityValidation.requireLivingEntity(h, "health check");
                    ctx.codegen().addImport(LIVING_ENTITY);
                    String op = ctx.java("op");
                    String n = ctx.java("n");
                    if (known) return "((LivingEntity) " + h.java() + ").getHealth() " + op + " " + n;
                    return "(" + h.java() + " instanceof LivingEntity _le && _le.getHealth() " + op + " " + n + ")";
                }));
    }
}
