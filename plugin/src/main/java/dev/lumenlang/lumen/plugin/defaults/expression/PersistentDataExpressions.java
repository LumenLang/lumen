package dev.lumenlang.lumen.plugin.defaults.expression;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.CodegenAccess;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.Types;
import dev.lumenlang.lumen.plugin.Lumen;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * Registers all persistent data container expression patterns for entities, items, and blocks.
 */
@Registration
@SuppressWarnings("unused")
public final class PersistentDataExpressions {

    private static final String NAMESPACED_KEY = NamespacedKey.class.getName();
    private static final String PDC_TYPE = PersistentDataType.class.getName();
    private static final String LUMEN = Lumen.class.getName();

    private static void pdcImports(@NotNull CodegenAccess c) {
        c.addImport(NAMESPACED_KEY);
        c.addImport(PDC_TYPE);
        c.addImport(LUMEN);
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen").pattern("get pdc %key:STRING% of %e:ENTITY%")
                .description("Retrieves a string value from an entity's PersistentDataContainer.")
                .example("set val to get pdc \"myKey\" of entity")
                .since("1.0.0").category(Categories.ENTITY)
                .returnJavaType(Types.STRING)
                .handler(ctx -> {
                    pdcImports(ctx.codegen());
                    return new ExpressionResult(
                            ctx.java("e") + ".getPersistentDataContainer().getOrDefault(new NamespacedKey(Lumen.instance(), "
                                    + ctx.java("key") + "), PersistentDataType.STRING, \"\")",
                            null, Types.STRING);
                }));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get pdc %key:STRING% of %i:ITEMSTACK%")
                .description("Retrieves a string value from an item stack's PersistentDataContainer.")
                .example("set val to get pdc \"custom_id\" of item")
                .since("1.0.0").category(Categories.ITEM)
                .returnJavaType(Types.STRING)
                .handler(ctx -> {
                    pdcImports(ctx.codegen());
                    String itemJava = ctx.java("i");
                    return new ExpressionResult(
                            "(" + itemJava + ".hasItemMeta() ? " + itemJava + ".getItemMeta().getPersistentDataContainer().getOrDefault(new NamespacedKey(Lumen.instance(), "
                                    + ctx.java("key") + "), PersistentDataType.STRING, \"\") : \"\")",
                            null, Types.STRING);
                }));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get pdc %key:STRING% of %bl:BLOCK%")
                .description("Retrieves a string value from a block's PersistentDataContainer.")
                .example("set val to get pdc \"owner\" of block")
                .since("1.0.0").category(Categories.BLOCK)
                .returnJavaType(Types.STRING)
                .handler(ctx -> {
                    pdcImports(ctx.codegen());
                    return new ExpressionResult(
                            ctx.java("bl") + ".getState().getPersistentDataContainer().getOrDefault(new NamespacedKey(Lumen.instance(), "
                                    + ctx.java("key") + "), PersistentDataType.STRING, \"\")",
                            null, Types.STRING);
                }));
    }
}
