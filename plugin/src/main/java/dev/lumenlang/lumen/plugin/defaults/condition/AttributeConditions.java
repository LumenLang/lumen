package dev.lumenlang.lumen.plugin.defaults.condition;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.CodegenAccess;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess.VarHandle;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.plugin.util.EntityValidation;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings("unused")
public final class AttributeConditions {

    private static final String LIVING = LivingEntity.class.getName();

    private static void imports(@NotNull CodegenAccess codegen) {
        codegen.addImport(LIVING);
        codegen.addImport(Attribute.class.getName());
        codegen.addImport(AttributeInstance.class.getName());
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%e:ENTITY_POSSESSIVE% %attr:ATTRIBUTE% [value] %op:OP% %val:DOUBLE%")
                .description("Compares a living entity's attribute value against a number using a relational operator.")
                .example("if mob's max_health > 20:")
                .since("1.0.0")
                .category(Categories.ATTRIBUTE)
                .handler((match, env, ctx) -> {
                    VarHandle h = match.ref("e");
                    boolean known = EntityValidation.requireSubtype(h, LIVING, "compare attribute");
                    imports(ctx);
                    String java = h.java();
                    String attr = match.java("attr", ctx, env);
                    String op = match.java("op", ctx, env);
                    String val = match.java("val", ctx, env);
                    if (known) {
                        String le = "((LivingEntity) " + java + ")";
                        return "(" + le + ".getAttribute(" + attr + ") != null"
                                + " && " + le + ".getAttribute(" + attr + ").getValue() " + op + " " + val + ")";
                    }
                    return "(" + java + " instanceof LivingEntity _le"
                            + " && _le.getAttribute(" + attr + ") != null"
                            + " && _le.getAttribute(" + attr + ").getValue() " + op + " " + val + ")";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%e:ENTITY% has %attr:ATTRIBUTE%")
                .description("Checks if a living entity has the specified attribute.")
                .example("if mob has max_health:")
                .since("1.0.0")
                .category(Categories.ATTRIBUTE)
                .handler((match, env, ctx) -> {
                    VarHandle h = match.ref("e");
                    boolean known = EntityValidation.requireSubtype(h, LIVING, "has attribute");
                    imports(ctx);
                    String java = h.java();
                    String attr = match.java("attr", ctx, env);
                    if (known) {
                        return "(((LivingEntity) " + java + ").getAttribute(" + attr + ") != null)";
                    }
                    return "(" + java + " instanceof LivingEntity _le && _le.getAttribute(" + attr + ") != null)";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%e:ENTITY% (does not have|doesn't have|lacks) %attr:ATTRIBUTE%")
                .description("Checks if a living entity does not have the specified attribute.")
                .example("if mob lacks attack_damage:")
                .since("1.0.0")
                .category(Categories.ATTRIBUTE)
                .handler((match, env, ctx) -> {
                    VarHandle h = match.ref("e");
                    boolean known = EntityValidation.requireSubtype(h, LIVING, "lacks attribute");
                    imports(ctx);
                    String java = h.java();
                    String attr = match.java("attr", ctx, env);
                    if (known) {
                        return "(((LivingEntity) " + java + ").getAttribute(" + attr + ") == null)";
                    }
                    return "(!(" + java + " instanceof LivingEntity _le) || _le.getAttribute(" + attr + ") == null)";
                }));
    }
}
