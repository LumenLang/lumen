package dev.lumenlang.lumen.plugin.defaults.expression;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.CodegenAccess;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess.VarHandle;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.Types;
import dev.lumenlang.lumen.plugin.util.EntityValidation;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class AttributeExpressions {

    private static final String LIVING = LivingEntity.class.getName();

    private static void imports(@NotNull CodegenAccess codegen) {
        codegen.addImport(LIVING);
        codegen.addImport(Attribute.class.getName());
        codegen.addImport(AttributeInstance.class.getName());
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %e:ENTITY_POSSESSIVE% %attr:ATTRIBUTE% [base] [value]")
                .description("Returns the base value of an attribute on a living entity, or 0 if the entity does not have the attribute.")
                .example("set hp to mob's max_health")
                .since("1.0.0")
                .category(Categories.ATTRIBUTE)
                .returnJavaType(Types.DOUBLE)
                .handler(ctx -> {
                    VarHandle h = (VarHandle) ctx.value("e");
                    boolean known = EntityValidation.requireSubtype(h, LIVING, "get attribute base value");
                    imports(ctx.codegen());
                    String java = ctx.java("e");
                    String attr = ctx.java("attr");
                    if (known) {
                        String le = "((LivingEntity) " + java + ")";
                        return new ExpressionResult(
                                "(" + le + ".getAttribute(" + attr + ") != null ? " + le + ".getAttribute(" + attr + ").getBaseValue() : 0)", null, Types.DOUBLE);
                    }
                    return new ExpressionResult(
                            "(" + java + " instanceof LivingEntity _le && _le.getAttribute(" + attr + ") != null ? _le.getAttribute(" + attr + ").getBaseValue() : 0)", null, Types.DOUBLE);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %e:ENTITY_POSSESSIVE% %attr:ATTRIBUTE% (effective|total) [value]")
                .description("Returns the effective (total with modifiers) value of an attribute on a living entity, or 0 if the entity does not have the attribute.")
                .example("set totalHp to mob's max_health effective")
                .since("1.0.0")
                .category(Categories.ATTRIBUTE)
                .returnJavaType(Types.DOUBLE)
                .handler(ctx -> {
                    VarHandle h = (VarHandle) ctx.value("e");
                    boolean known = EntityValidation.requireSubtype(h, LIVING, "get attribute effective value");
                    imports(ctx.codegen());
                    String java = ctx.java("e");
                    String attr = ctx.java("attr");
                    if (known) {
                        String le = "((LivingEntity) " + java + ")";
                        return new ExpressionResult(
                                "(" + le + ".getAttribute(" + attr + ") != null ? " + le + ".getAttribute(" + attr + ").getValue() : 0)", null, Types.DOUBLE);
                    }
                    return new ExpressionResult(
                            "(" + java + " instanceof LivingEntity _le && _le.getAttribute(" + attr + ") != null ? _le.getAttribute(" + attr + ").getValue() : 0)", null, Types.DOUBLE);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %e:ENTITY_POSSESSIVE% %attr:ATTRIBUTE% default [value]")
                .description("Returns the default value of an attribute on a living entity, or 0 if the entity does not have the attribute.")
                .example("set defaultHp to mob's max_health default")
                .since("1.0.0")
                .category(Categories.ATTRIBUTE)
                .returnJavaType(Types.DOUBLE)
                .handler(ctx -> {
                    VarHandle h = (VarHandle) ctx.value("e");
                    boolean known = EntityValidation.requireSubtype(h, LIVING, "get attribute default value");
                    imports(ctx.codegen());
                    String java = ctx.java("e");
                    String attr = ctx.java("attr");
                    if (known) {
                        String le = "((LivingEntity) " + java + ")";
                        return new ExpressionResult(
                                "(" + le + ".getAttribute(" + attr + ") != null ? " + le + ".getAttribute(" + attr + ").getDefaultValue() : 0)", null, Types.DOUBLE);
                    }
                    return new ExpressionResult(
                            "(" + java + " instanceof LivingEntity _le && _le.getAttribute(" + attr + ") != null ? _le.getAttribute(" + attr + ").getDefaultValue() : 0)", null, Types.DOUBLE);
                }));
    }
}
