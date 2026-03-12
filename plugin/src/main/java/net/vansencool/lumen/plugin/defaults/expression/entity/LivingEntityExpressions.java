package net.vansencool.lumen.plugin.defaults.expression.entity;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.codegen.EnvironmentAccess.VarHandle;
import net.vansencool.lumen.api.handler.ExpressionHandler.ExpressionResult;
import net.vansencool.lumen.api.pattern.Categories;
import net.vansencool.lumen.api.type.Types;
import net.vansencool.lumen.plugin.defaults.util.AttributeNames;
import net.vansencool.lumen.plugin.util.EntityValidation;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Registers expression patterns that require a {@code LivingEntity}.
 */
@Registration
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class LivingEntityExpressions {

    private static final String LIVING_ENTITY = LivingEntity.class.getName();
    private static final String ATTRIBUTE = Attribute.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
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
                        return new ExpressionResult("((LivingEntity) " + java + ").getHealth()", null, Types.DOUBLE);
                    }
                    return new ExpressionResult(
                            "(" + java + " instanceof LivingEntity _le ? _le.getHealth() : 0.0)",
                            null, Types.DOUBLE);
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
                                null, Types.DOUBLE);
                    }
                    return new ExpressionResult(
                            "(" + java + " instanceof LivingEntity _le ? _le.getAttribute(Attribute." + attrName + ").getValue() : 0.0)",
                            null, Types.DOUBLE);
                }));
    }
}
