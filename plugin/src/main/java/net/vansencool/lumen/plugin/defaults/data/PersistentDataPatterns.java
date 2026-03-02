package net.vansencool.lumen.plugin.defaults.data;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.codegen.CodegenAccess;
import net.vansencool.lumen.api.handler.ExpressionHandler.ExpressionResult;
import net.vansencool.lumen.api.pattern.Categories;
import net.vansencool.lumen.plugin.Lumen;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

@Registration
@Description("Registers PersistentDataContainer patterns for entities, items, and blocks.")
@SuppressWarnings("unused")
public final class PersistentDataPatterns {

    private static final String NAMESPACED_KEY = NamespacedKey.class.getName();
    private static final String PDC_TYPE = PersistentDataType.class.getName();
    private static final String LUMEN = Lumen.class.getName();
    private static final String ITEM_META = ItemMeta.class.getName();
    private static final String BLOCK_STATE = BlockState.class.getName();

    private static void pdcImports(@NotNull CodegenAccess c) {
        c.addImport(NAMESPACED_KEY);
        c.addImport(PDC_TYPE);
        c.addImport(LUMEN);
    }

    private static void pdcItemImports(@NotNull CodegenAccess c) {
        pdcImports(c);
        c.addImport(ITEM_META);
    }

    private static void pdcBlockImports(@NotNull CodegenAccess c) {
        pdcImports(c);
        c.addImport(BLOCK_STATE);
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        entityPdc(api);
        itemPdc(api);
        blockPdc(api);
    }

    private void entityPdc(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set pdc %key:STRING% of %e:ENTITY% to %val:STRING%")
                .description("Sets a string value in an entity's PersistentDataContainer.")
                .example("set pdc \"myKey\" of entity to \"myValue\"")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((line, ctx, out) -> {
                    pdcImports(ctx.codegen());
                    out.line("{");
                    out.line("NamespacedKey __key = new NamespacedKey(Lumen.instance(), " + ctx.java("key") + ");");
                    out.line(ctx.java("e") + ".getPersistentDataContainer().set(__key, PersistentDataType.STRING, " + ctx.java("val") + ");");
                    out.line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(remove|delete) pdc %key:STRING% from %e:ENTITY%")
                .description("Removes a key from an entity's PersistentDataContainer.")
                .example("remove pdc \"myKey\" from entity")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler((line, ctx, out) -> {
                    pdcImports(ctx.codegen());
                    out.line("{");
                    out.line("NamespacedKey __key = new NamespacedKey(Lumen.instance(), " + ctx.java("key") + ");");
                    out.line(ctx.java("e") + ".getPersistentDataContainer().remove(__key);");
                    out.line("}");
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% has pdc %key:STRING%")
                .description("Checks if an entity's PersistentDataContainer has a given key.")
                .example("if entity has pdc \"myKey\":")
                .since("1.0.0").category(Categories.ENTITY)
                .handler((match, env, ctx) -> {
                    pdcImports(ctx);
                    return match.ref("e").java() + ".getPersistentDataContainer().has(new NamespacedKey(Lumen.instance(), "
                            + match.java("key", ctx, env) + "), PersistentDataType.STRING)";
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%e:ENTITY% (does not have|has no) pdc %key:STRING%")
                .description("Checks if an entity's PersistentDataContainer does not have a given key.")
                .example("if entity does not have pdc \"myKey\":")
                .since("1.0.0").category(Categories.ENTITY)
                .handler((match, env, ctx) -> {
                    pdcImports(ctx);
                    return "!" + match.ref("e").java() + ".getPersistentDataContainer().has(new NamespacedKey(Lumen.instance(), "
                            + match.java("key", ctx, env) + "), PersistentDataType.STRING)";
                }));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get pdc %key:STRING% of %e:ENTITY%")
                .description("Retrieves a string value from an entity's PersistentDataContainer.")
                .example("var val = pdc \"myKey\" of entity")
                .since("1.0.0").category(Categories.ENTITY)
                .handler(ctx -> {
                    pdcImports(ctx.codegen());
                    return new ExpressionResult(
                            ctx.java("e") + ".getPersistentDataContainer().getOrDefault(new NamespacedKey(Lumen.instance(), "
                                    + ctx.java("key") + "), PersistentDataType.STRING, \"\")",
                            null);
                }));
    }

    private void itemPdc(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set pdc %key:STRING% of %i:ITEMSTACK% to %val:STRING%")
                .description("Sets a string value in an item stack's PersistentDataContainer.")
                .example("set pdc \"custom_id\" of item to \"magic_wand\"")
                .since("1.0.0")
                .category(Categories.ITEM)
                .handler((line, ctx, out) -> {
                    pdcItemImports(ctx.codegen());
                    String itemJava = ctx.java("i");
                    out.line("{");
                    out.line("NamespacedKey __key = new NamespacedKey(Lumen.instance(), " + ctx.java("key") + ");");
                    out.line("ItemMeta __meta = " + itemJava + ".getItemMeta();");
                    out.line("__meta.getPersistentDataContainer().set(__key, PersistentDataType.STRING, " + ctx.java("val") + ");");
                    out.line(itemJava + ".setItemMeta(__meta);");
                    out.line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(remove|delete) pdc %key:STRING% from %i:ITEMSTACK%")
                .description("Removes a key from an item stack's PersistentDataContainer.")
                .example("remove pdc \"custom_id\" from item")
                .since("1.0.0")
                .category(Categories.ITEM)
                .handler((line, ctx, out) -> {
                    pdcItemImports(ctx.codegen());
                    String itemJava = ctx.java("i");
                    out.line("{");
                    out.line("NamespacedKey __key = new NamespacedKey(Lumen.instance(), " + ctx.java("key") + ");");
                    out.line("ItemMeta __meta = " + itemJava + ".getItemMeta();");
                    out.line("__meta.getPersistentDataContainer().remove(__key);");
                    out.line(itemJava + ".setItemMeta(__meta);");
                    out.line("}");
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%i:ITEMSTACK% has pdc %key:STRING%")
                .description("Checks if an item stack's PersistentDataContainer has a given key.")
                .example("if item has pdc \"custom_id\":")
                .since("1.0.0").category(Categories.ITEM)
                .handler((match, env, ctx) -> {
                    pdcImports(ctx);
                    return "(" + match.ref("i").java() + ".hasItemMeta() && " + match.ref("i").java()
                            + ".getItemMeta().getPersistentDataContainer().has(new NamespacedKey(Lumen.instance(), "
                            + match.java("key", ctx, env) + "), PersistentDataType.STRING))";
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%i:ITEMSTACK% (does not have|has no) pdc %key:STRING%")
                .description("Checks if an item stack's PersistentDataContainer does not have a given key.")
                .example("if item does not have pdc \"custom_id\":")
                .since("1.0.0").category(Categories.ITEM)
                .handler((match, env, ctx) -> {
                    pdcImports(ctx);
                    return "(!(" + match.ref("i").java() + ".hasItemMeta() && " + match.ref("i").java()
                            + ".getItemMeta().getPersistentDataContainer().has(new NamespacedKey(Lumen.instance(), "
                            + match.java("key", ctx, env) + "), PersistentDataType.STRING)))";
                }));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get pdc %key:STRING% of %i:ITEMSTACK%")
                .description("Retrieves a string value from an item stack's PersistentDataContainer.")
                .example("var val = pdc \"custom_id\" of item")
                .since("1.0.0").category(Categories.ITEM)
                .handler(ctx -> {
                    pdcImports(ctx.codegen());
                    String itemJava = ctx.java("i");
                    return new ExpressionResult(
                            "(" + itemJava + ".hasItemMeta() ? " + itemJava + ".getItemMeta().getPersistentDataContainer().getOrDefault(new NamespacedKey(Lumen.instance(), "
                                    + ctx.java("key") + "), PersistentDataType.STRING, \"\") : \"\")",
                            null);
                }));
    }

    private void blockPdc(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set pdc %key:STRING% of %bl:BLOCK% to %val:STRING%")
                .description("Sets a string value in a block's (tile entity) PersistentDataContainer. Only works on blocks with a tile entity (chests, signs, etc.).")
                .example("set pdc \"owner\" of block to player's name")
                .since("1.0.0")
                .category(Categories.BLOCK)
                .handler((line, ctx, out) -> {
                    pdcBlockImports(ctx.codegen());
                    String blockJava = ctx.java("bl");
                    out.line("{");
                    out.line("BlockState __state = " + blockJava + ".getState();");
                    out.line("NamespacedKey __key = new NamespacedKey(Lumen.instance(), " + ctx.java("key") + ");");
                    out.line("__state.getPersistentDataContainer().set(__key, PersistentDataType.STRING, " + ctx.java("val") + ");");
                    out.line("__state.update();");
                    out.line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(remove|delete) pdc %key:STRING% from %bl:BLOCK%")
                .description("Removes a key from a block's PersistentDataContainer.")
                .example("remove pdc \"owner\" from block")
                .since("1.0.0")
                .category(Categories.BLOCK)
                .handler((line, ctx, out) -> {
                    pdcBlockImports(ctx.codegen());
                    String blockJava = ctx.java("bl");
                    out.line("{");
                    out.line("BlockState __state = " + blockJava + ".getState();");
                    out.line("NamespacedKey __key = new NamespacedKey(Lumen.instance(), " + ctx.java("key") + ");");
                    out.line("__state.getPersistentDataContainer().remove(__key);");
                    out.line("__state.update();");
                    out.line("}");
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%bl:BLOCK% has pdc %key:STRING%")
                .description("Checks if a block's PersistentDataContainer has a given key.")
                .example("if block has pdc \"owner\":")
                .since("1.0.0").category(Categories.BLOCK)
                .handler((match, env, ctx) -> {
                    pdcImports(ctx);
                    return match.ref("bl").java() + ".getState().getPersistentDataContainer().has(new NamespacedKey(Lumen.instance(), "
                            + match.java("key", ctx, env) + "), PersistentDataType.STRING)";
                }));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%bl:BLOCK% (does not have|has no) pdc %key:STRING%")
                .description("Checks if a block's PersistentDataContainer does not have a given key.")
                .example("if block does not have pdc \"owner\":")
                .since("1.0.0").category(Categories.BLOCK)
                .handler((match, env, ctx) -> {
                    pdcImports(ctx);
                    return "!" + match.ref("bl").java() + ".getState().getPersistentDataContainer().has(new NamespacedKey(Lumen.instance(), "
                            + match.java("key", ctx, env) + "), PersistentDataType.STRING)";
                }));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get pdc %key:STRING% of %bl:BLOCK%")
                .description("Retrieves a string value from a block's PersistentDataContainer.")
                .example("var val = pdc \"owner\" of block")
                .since("1.0.0").category(Categories.BLOCK)
                .handler(ctx -> {
                    pdcImports(ctx.codegen());
                    return new ExpressionResult(
                            ctx.java("bl") + ".getState().getPersistentDataContainer().getOrDefault(new NamespacedKey(Lumen.instance(), "
                                    + ctx.java("key") + "), PersistentDataType.STRING, \"\")",
                            null);
                }));
    }
}
