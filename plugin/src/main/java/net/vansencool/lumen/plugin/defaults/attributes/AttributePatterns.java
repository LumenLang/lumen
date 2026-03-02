package net.vansencool.lumen.plugin.defaults.attributes;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.codegen.CodegenAccess;
import net.vansencool.lumen.api.codegen.EnvironmentAccess.VarHandle;
import net.vansencool.lumen.api.codegen.JavaOutput;
import net.vansencool.lumen.api.handler.ExpressionHandler.ExpressionResult;
import net.vansencool.lumen.api.pattern.Categories;
import net.vansencool.lumen.plugin.util.EntityValidation;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

/**
 * Registers statement, condition, and expression patterns for manipulating
 * entity attribute values.
 *
 * <p>
 * All patterns operate on {@code LivingEntity} and use the {@code ATTRIBUTE}
 * type binding to resolve attribute names in a version-aware way via
 * {@link AttributeNames}.
 *
 * <h2>Statements</h2>
 * <ul>
 * <li>{@code set <entity> <attribute> [base] to <value>}</li>
 * <li>{@code reset <entity> <attribute>}</li>
 * </ul>
 *
 * <h2>Conditions</h2>
 * <ul>
 * <li>{@code <entity> <attribute> [value] is <op> <value>}</li>
 * </ul>
 *
 * <h2>Expressions</h2>
 * <ul>
 * <li>{@code <entity> <attribute> [base] [value]}</li>
 * <li>{@code <entity> <attribute> default [value]}</li>
 * </ul>
 *
 * @see AttributeBinding
 * @see AttributeNames
 */
@Registration
@Description("Registers attribute patterns: set, get, reset, and compare attribute values.")
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class AttributePatterns {

    private static final String LIVING = LivingEntity.class.getName();

    /**
     * Returns a Java expression that obtains a {@code LivingEntity} reference
     * from the given entity variable. When the entity is already known to be a
     * {@code LivingEntity} subtype (e.g. Player), a direct cast is used. Otherwise,
     * the expression is left as-is for use inside an {@code instanceof} guard.
     *
     * @param h    the entity variable handle
     * @param java the raw Java variable expression
     * @return a Java expression that evaluates to {@code LivingEntity}
     */
    private static @NotNull String livingExpr(@NotNull VarHandle h, @NotNull String java) {
        boolean known = EntityValidation.requireSubtype(h, LIVING, "attribute");
        return known ? "((LivingEntity) " + java + ")" : java;
    }

    /**
     * Emits a statement block guarded by a LivingEntity check. When the entity is
     * already known to be a LivingEntity subtype, no instanceof guard is emitted.
     */
    private static void emitGuarded(@NotNull VarHandle h, @NotNull String java, @NotNull JavaOutput out, @NotNull GuardedBlock body) {
        boolean known = EntityValidation.requireSubtype(h, LIVING, "attribute");
        if (known) {
            body.emit("((LivingEntity) " + java + ")");
        } else {
            out.line("if (" + java + " instanceof LivingEntity _le) {");
            body.emit("_le");
            out.line("}");
        }
    }

    private static void imports(@NotNull CodegenAccess codegen) {
        codegen.addImport(LIVING);
        codegen.addImport(Attribute.class.getName());
        codegen.addImport(AttributeInstance.class.getName());
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        registerStatements(api);
        registerConditions(api);
        registerExpressions(api);
    }

    private void registerStatements(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %e:ENTITY_POSSESSIVE% %attr:ATTRIBUTE% [base] [value] [to] %val:EXPR%")
                .description("Sets the base value of an attribute on a living entity.")
                .example("set mob's max_health to 40")
                .since("1.0.0")
                .category(Categories.ATTRIBUTE)
                .handler((line, ctx, out) -> {
                    imports(ctx.codegen());
                    String java = ctx.java("e");
                    String attr = ctx.java("attr");
                    String val = ctx.java("val");
                    emitGuarded((VarHandle) ctx.value("e"), java, out, le -> {
                        out.line("    AttributeInstance _ai = " + le + ".getAttribute(" + attr
                                + ");");
                        out.line("    if (_ai != null) _ai.setBaseValue(" + val + ");");
                    });
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("reset %e:ENTITY_POSSESSIVE% %attr:ATTRIBUTE%")
                .description("Resets an entity's attribute base value to its default value.")
                .example("reset mob's max_health")
                .since("1.0.0")
                .category(Categories.ATTRIBUTE)
                .handler((line, ctx, out) -> {
                    imports(ctx.codegen());
                    String java = ctx.java("e");
                    String attr = ctx.java("attr");
                    emitGuarded((VarHandle) ctx.value("e"), java, out, le -> {
                        out.line("    AttributeInstance _ai = " + le + ".getAttribute(" + attr
                                + ");");
                        out.line("    if (_ai != null) _ai.setBaseValue(_ai.getDefaultValue());");
                    });
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("add %val:EXPR% [to] %e:ENTITY_POSSESSIVE% %attr:ATTRIBUTE%")
                .description("Adds a value to an entity's attribute base value.")
                .example("add 10 to mob's max_health")
                .since("1.0.0")
                .category(Categories.ATTRIBUTE)
                .handler((line, ctx, out) -> {
                    imports(ctx.codegen());
                    String java = ctx.java("e");
                    String attr = ctx.java("attr");
                    String val = ctx.java("val");
                    emitGuarded((VarHandle) ctx.value("e"), java, out, le -> {
                        out.line("    AttributeInstance _ai = " + le + ".getAttribute(" + attr
                                + ");");
                        out.line("    if (_ai != null) _ai.setBaseValue(_ai.getBaseValue() + "
                                + val + ");");
                    });
                }));
    }

    private void registerConditions(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%e:ENTITY_POSSESSIVE% %attr:ATTRIBUTE% [value] %op:OP% %val:EXPR%")
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
                                + " && " + le + ".getAttribute(" + attr
                                + ").getValue() " + op + " " + val + ")";
                    }
                    return "(" + java + " instanceof LivingEntity _le"
                            + " && _le.getAttribute(" + attr + ") != null"
                            + " && _le.getAttribute(" + attr + ").getValue() " + op + " "
                            + val + ")";
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
                        return "(((LivingEntity) " + java + ").getAttribute(" + attr
                                + ") != null)";
                    }
                    return "(" + java + " instanceof LivingEntity _le"
                            + " && _le.getAttribute(" + attr + ") != null)";
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
                        return "(((LivingEntity) " + java + ").getAttribute(" + attr
                                + ") == null)";
                    }
                    return "(!(" + java + " instanceof LivingEntity _le)"
                            + " || _le.getAttribute(" + attr + ") == null)";
                }));
    }

    private void registerExpressions(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %e:ENTITY_POSSESSIVE% %attr:ATTRIBUTE% [base] [value]")
                .description(
                        "Returns the base value of an attribute on a living entity, or 0 if the entity does not have the attribute.")
                .example("var hp = mob's max_health")
                .since("1.0.0")
                .category(Categories.ATTRIBUTE)
                .handler(ctx -> {
                    VarHandle h = (VarHandle) ctx.value("e");
                    boolean known = EntityValidation.requireSubtype(h, LIVING,
                            "get attribute base value");
                    imports(ctx.codegen());
                    String java = ctx.java("e");
                    String attr = ctx.java("attr");
                    if (known) {
                        String le = "((LivingEntity) " + java + ")";
                        return new ExpressionResult(
                                "(" + le + ".getAttribute(" + attr + ") != null"
                                        + " ? " + le + ".getAttribute(" + attr
                                        + ").getBaseValue() : 0)",
                                null);
                    }
                    return new ExpressionResult(
                            "(" + java + " instanceof LivingEntity _le"
                                    + " && _le.getAttribute(" + attr + ") != null"
                                    + " ? _le.getAttribute(" + attr
                                    + ").getBaseValue() : 0)",
                            null);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %e:ENTITY_POSSESSIVE% %attr:ATTRIBUTE% (effective|total) [value]")
                .description(
                        "Returns the effective (total with modifiers) value of an attribute on a living entity, or 0 if the entity does not have the attribute.")
                .example("var totalHp = mob's max_health effective")
                .since("1.0.0")
                .category(Categories.ATTRIBUTE)
                .handler(ctx -> {
                    VarHandle h = (VarHandle) ctx.value("e");
                    boolean known = EntityValidation.requireSubtype(h, LIVING,
                            "get attribute effective value");
                    imports(ctx.codegen());
                    String java = ctx.java("e");
                    String attr = ctx.java("attr");
                    if (known) {
                        String le = "((LivingEntity) " + java + ")";
                        return new ExpressionResult(
                                "(" + le + ".getAttribute(" + attr + ") != null"
                                        + " ? " + le + ".getAttribute(" + attr
                                        + ").getValue() : 0)",
                                null);
                    }
                    return new ExpressionResult(
                            "(" + java + " instanceof LivingEntity _le"
                                    + " && _le.getAttribute(" + attr + ") != null"
                                    + " ? _le.getAttribute(" + attr
                                    + ").getValue() : 0)",
                            null);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get %e:ENTITY_POSSESSIVE% %attr:ATTRIBUTE% default [value]")
                .description(
                        "Returns the default value of an attribute on a living entity, or 0 if the entity does not have the attribute.")
                .example("var defaultHp = mob's max_health default")
                .since("1.0.0")
                .category(Categories.ATTRIBUTE)
                .handler(ctx -> {
                    VarHandle h = (VarHandle) ctx.value("e");
                    boolean known = EntityValidation.requireSubtype(h, LIVING,
                            "get attribute default value");
                    imports(ctx.codegen());
                    String java = ctx.java("e");
                    String attr = ctx.java("attr");
                    if (known) {
                        String le = "((LivingEntity) " + java + ")";
                        return new ExpressionResult(
                                "(" + le + ".getAttribute(" + attr + ") != null"
                                        + " ? " + le + ".getAttribute(" + attr
                                        + ").getDefaultValue() : 0)",
                                null);
                    }
                    return new ExpressionResult(
                            "(" + java + " instanceof LivingEntity _le"
                                    + " && _le.getAttribute(" + attr + ") != null"
                                    + " ? _le.getAttribute(" + attr
                                    + ").getDefaultValue() : 0)",
                            null);
                }));
    }

    @FunctionalInterface
    private interface GuardedBlock {
        void emit(@NotNull String leVar);
    }
}
