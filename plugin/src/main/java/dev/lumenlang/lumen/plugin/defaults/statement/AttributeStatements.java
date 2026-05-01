package dev.lumenlang.lumen.plugin.defaults.statement;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.CodegenContext;
import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.codegen.TypeEnv.VarHandle;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.plugin.util.EntityValidation;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

@Registration
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class AttributeStatements {

    private static final String LIVING = LivingEntity.class.getName();

    private static void emitGuarded(@NotNull VarHandle h, @NotNull String java, @NotNull HandlerContext ctx, @NotNull GuardedBlock body) {
        boolean known = EntityValidation.requireSubtype(h, LIVING, "attribute");
        if (known) {
            body.emit("((LivingEntity) " + java + ")");
        } else {
            ctx.out().line("if (" + java + " instanceof LivingEntity _le) {");
            body.emit("_le");
            ctx.out().line("}");
        }
    }

    private static void imports(@NotNull CodegenContext codegen) {
        codegen.addImport(LIVING);
        codegen.addImport(Attribute.class.getName());
        codegen.addImport(AttributeInstance.class.getName());
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %e:ENTITY_POSSESSIVE% %attr:ATTRIBUTE% [base] [value] [to] %val:DOUBLE%")
                .description("Sets the base value of an attribute on a living entity.")
                .example("set mob's max_health to 40")
                .since("1.0.0")
                .category(Categories.ATTRIBUTE)
                .handler(ctx -> {
                    imports(ctx.codegen());
                    String java = ctx.java("e");
                    String attr = ctx.java("attr");
                    String val = ctx.java("val");
                    emitGuarded((VarHandle) ctx.value("e"), java, ctx, le -> {
                        ctx.out().line("    AttributeInstance _ai = " + le + ".getAttribute(" + attr + ");");
                        ctx.out().line("    if (_ai != null) _ai.setBaseValue(" + val + ");");
                    });
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("reset %e:ENTITY_POSSESSIVE% %attr:ATTRIBUTE%")
                .description("Resets an entity's attribute base value to its default value.")
                .example("reset mob's max_health")
                .since("1.0.0")
                .category(Categories.ATTRIBUTE)
                .handler(ctx -> {
                    imports(ctx.codegen());
                    String java = ctx.java("e");
                    String attr = ctx.java("attr");
                    emitGuarded((VarHandle) ctx.value("e"), java, ctx, le -> {
                        ctx.out().line("    AttributeInstance _ai = " + le + ".getAttribute(" + attr + ");");
                        ctx.out().line("    if (_ai != null) _ai.setBaseValue(_ai.getDefaultValue());");
                    });
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("add %val:DOUBLE% [to] %e:ENTITY_POSSESSIVE% %attr:ATTRIBUTE%")
                .description("Adds a value to an entity's attribute base value.")
                .example("add 10 to mob's max_health")
                .since("1.0.0")
                .category(Categories.ATTRIBUTE)
                .handler(ctx -> {
                    imports(ctx.codegen());
                    String java = ctx.java("e");
                    String attr = ctx.java("attr");
                    String val = ctx.java("val");
                    emitGuarded((VarHandle) ctx.value("e"), java, ctx, le -> {
                        ctx.out().line("    AttributeInstance _ai = " + le + ".getAttribute(" + attr + ");");
                        ctx.out().line("    if (_ai != null) _ai.setBaseValue(_ai.getBaseValue() + " + val + ");");
                    });
                }));
    }

    @FunctionalInterface
    private interface GuardedBlock {
        void emit(@NotNull String leVar);
    }
}
