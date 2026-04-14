package dev.lumenlang.lumen.plugin.defaults.statement;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.CodegenAccess;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.plugin.Lumen;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * Registers all persistent data container statement patterns for entities, items, and blocks.
 */
@Registration
@SuppressWarnings("unused")
public final class PersistentDataStatements {

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
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set pdc %key:STRING% of %e:ENTITY% to %val:STRING%")
                .description("Sets a string value in an entity's PersistentDataContainer.")
                .example("set pdc \"myKey\" of entity to \"myValue\"")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    pdcImports(ctx.codegen());
                    ctx.out().line("{");
                    ctx.out().line("NamespacedKey __key = new NamespacedKey(Lumen.instance(), " + ctx.java("key") + ");");
                    ctx.out().line(ctx.java("e") + ".getPersistentDataContainer().set(__key, PersistentDataType.STRING, " + ctx.java("val") + ");");
                    ctx.out().line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(remove|delete) pdc %key:STRING% from %e:ENTITY%")
                .description("Removes a key from an entity's PersistentDataContainer.")
                .example("remove pdc \"myKey\" from entity")
                .since("1.0.0")
                .category(Categories.ENTITY)
                .handler(ctx -> {
                    pdcImports(ctx.codegen());
                    ctx.out().line("{");
                    ctx.out().line("NamespacedKey __key = new NamespacedKey(Lumen.instance(), " + ctx.java("key") + ");");
                    ctx.out().line(ctx.java("e") + ".getPersistentDataContainer().remove(__key);");
                    ctx.out().line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set pdc %key:STRING% of %i:ITEMSTACK% to %val:STRING%")
                .description("Sets a string value in an item stack's PersistentDataContainer.")
                .example("set pdc \"custom_id\" of item to \"magic_wand\"")
                .since("1.0.0")
                .category(Categories.ITEM)
                .handler(ctx -> {
                    pdcItemImports(ctx.codegen());
                    String itemJava = ctx.java("i");
                    ctx.out().line("{");
                    ctx.out().line("NamespacedKey __key = new NamespacedKey(Lumen.instance(), " + ctx.java("key") + ");");
                    ctx.out().line("ItemMeta __meta = " + itemJava + ".getItemMeta();");
                    ctx.out().line("__meta.getPersistentDataContainer().set(__key, PersistentDataType.STRING, " + ctx.java("val") + ");");
                    ctx.out().line(itemJava + ".setItemMeta(__meta);");
                    ctx.out().line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(remove|delete) pdc %key:STRING% from %i:ITEMSTACK%")
                .description("Removes a key from an item stack's PersistentDataContainer.")
                .example("remove pdc \"custom_id\" from item")
                .since("1.0.0")
                .category(Categories.ITEM)
                .handler(ctx -> {
                    pdcItemImports(ctx.codegen());
                    String itemJava = ctx.java("i");
                    ctx.out().line("{");
                    ctx.out().line("NamespacedKey __key = new NamespacedKey(Lumen.instance(), " + ctx.java("key") + ");");
                    ctx.out().line("ItemMeta __meta = " + itemJava + ".getItemMeta();");
                    ctx.out().line("__meta.getPersistentDataContainer().remove(__key);");
                    ctx.out().line(itemJava + ".setItemMeta(__meta);");
                    ctx.out().line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set pdc %key:STRING% of %bl:BLOCK% to %val:STRING%")
                .description("Sets a string value in a block's (tile entity) PersistentDataContainer. Only works on blocks with a tile entity (chests, signs, etc.).")
                .example("set pdc \"owner\" of block to player's name")
                .since("1.0.0")
                .category(Categories.BLOCK)
                .handler(ctx -> {
                    pdcBlockImports(ctx.codegen());
                    String blockJava = ctx.java("bl");
                    ctx.out().line("{");
                    ctx.out().line("BlockState __state = " + blockJava + ".getState();");
                    ctx.out().line("NamespacedKey __key = new NamespacedKey(Lumen.instance(), " + ctx.java("key") + ");");
                    ctx.out().line("__state.getPersistentDataContainer().set(__key, PersistentDataType.STRING, " + ctx.java("val") + ");");
                    ctx.out().line("__state.update();");
                    ctx.out().line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(remove|delete) pdc %key:STRING% from %bl:BLOCK%")
                .description("Removes a key from a block's PersistentDataContainer.")
                .example("remove pdc \"owner\" from block")
                .since("1.0.0")
                .category(Categories.BLOCK)
                .handler(ctx -> {
                    pdcBlockImports(ctx.codegen());
                    String blockJava = ctx.java("bl");
                    ctx.out().line("{");
                    ctx.out().line("BlockState __state = " + blockJava + ".getState();");
                    ctx.out().line("NamespacedKey __key = new NamespacedKey(Lumen.instance(), " + ctx.java("key") + ");");
                    ctx.out().line("__state.getPersistentDataContainer().remove(__key);");
                    ctx.out().line("__state.update();");
                    ctx.out().line("}");
                }));
    }
}
