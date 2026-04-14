package dev.lumenlang.lumen.plugin.defaults.statement;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.pattern.Categories;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Registers statement patterns for manipulating item stacks: display name, lore,
 * amount, type, enchantments, durability, and custom model data.
 */
@Registration
@SuppressWarnings("unused")
public final class ItemStackStatements {

    private static final String ITEM_META = ItemMeta.class.getName();
    private static final String DAMAGEABLE = Damageable.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %i:ITEMSTACK_POSSESSIVE% (display name|name) [to] %name:STRING%")
                .description("Sets the display name of an item stack. Supports color codes via LumenText.")
                .example("set item's display name to \"<gold>Golden Sword\"")
                .since("1.0.0")
                .category(Categories.ITEM)
                .handler(ctx -> {
                    ctx.codegen().addImport(ITEM_META);
                    String itemJava = ctx.java("i");
                    ctx.out().line("{");
                    ctx.out().line("ItemMeta __meta = " + itemJava + ".getItemMeta();");
                    ctx.out().line("__meta.setDisplayName(LumenText.colorize(" + ctx.java("name") + "));");
                    ctx.out().line(itemJava + ".setItemMeta(__meta);");
                    ctx.out().line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %i:ITEMSTACK_POSSESSIVE% lore [to] %lore:STRING%")
                .description("Sets the lore of an item stack. Use | to separate lines. Supports MiniColorize.")
                .example("set item's lore to \"<gray>Golden Sword|<yellow>Quite cool huh?\"")
                .since("1.0.0")
                .category(Categories.ITEM)
                .handler(ctx -> {
                    ctx.codegen().addImport(ITEM_META);
                    ctx.codegen().addImport(Arrays.class.getName());
                    ctx.codegen().addImport(Collectors.class.getName());
                    String itemJava = ctx.java("i");
                    ctx.out().line("{");
                    ctx.out().line("ItemMeta __meta = " + itemJava + ".getItemMeta();");
                    ctx.out().line("__meta.setLore(Arrays.stream((" + ctx.java("lore") + ").split(\"\\\\|\")).map(LumenText::colorize).collect(Collectors.toList()));");
                    ctx.out().line(itemJava + ".setItemMeta(__meta);");
                    ctx.out().line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("add lore %line:STRING% to %i:ITEMSTACK%")
                .description("Appends a single lore line to an item stack. Supports MiniColorize.")
                .example("add lore \"<green>This item is special\" to item")
                .since("1.0.0")
                .category(Categories.ITEM)
                .handler(ctx -> {
                    ctx.codegen().addImport(ITEM_META);
                    ctx.codegen().addImport(List.class.getName());
                    ctx.codegen().addImport(ArrayList.class.getName());
                    String itemJava = ctx.java("i");
                    ctx.out().line("{");
                    ctx.out().line("ItemMeta __meta = " + itemJava + ".getItemMeta();");
                    ctx.out().line("List<String> __lore = __meta.hasLore() ? new ArrayList<>(__meta.getLore()) : new ArrayList<>();");
                    ctx.out().line("__lore.add(LumenText.colorize(" + ctx.java("line") + "));");
                    ctx.out().line("__meta.setLore(__lore);");
                    ctx.out().line(itemJava + ".setItemMeta(__meta);");
                    ctx.out().line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(clear|remove|wipe) %i:ITEMSTACK_POSSESSIVE% lore")
                .description("Removes all lore from an item stack.")
                .example("clear item's lore")
                .since("1.0.0")
                .category(Categories.ITEM)
                .handler(ctx -> {
                    ctx.codegen().addImport(ITEM_META);
                    String itemJava = ctx.java("i");
                    ctx.out().line("{");
                    ctx.out().line("ItemMeta __meta = " + itemJava + ".getItemMeta();");
                    ctx.out().line("__meta.setLore(null);");
                    ctx.out().line(itemJava + ".setItemMeta(__meta);");
                    ctx.out().line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %i:ITEMSTACK_POSSESSIVE% amount [to] %amt:INT%")
                .description("Sets the stack amount of an item.")
                .example("set item's amount to 32")
                .since("1.0.0")
                .category(Categories.ITEM)
                .handler(ctx ->
                        ctx.out().line(ctx.java("i") + ".setAmount(" + ctx.java("amt") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %i:ITEMSTACK_POSSESSIVE% type [to] %mat:MATERIAL%")
                .description("Changes the material type of an item stack.")
                .example("set item's type to diamond_sword")
                .since("1.0.0")
                .category(Categories.ITEM)
                .handler(ctx ->
                        ctx.out().line(ctx.java("i") + ".setType(" + ctx.java("mat") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %i:ITEMSTACK_POSSESSIVE% custom model data [to] %val:INT%")
                .description("Sets the custom model data of an item stack.")
                .example("set item's custom model data to 1001")
                .since("1.0.0")
                .category(Categories.ITEM)
                .handler(ctx -> {
                    ctx.codegen().addImport(ITEM_META);
                    String itemJava = ctx.java("i");
                    ctx.out().line("{");
                    ctx.out().line("ItemMeta __meta = " + itemJava + ".getItemMeta();");
                    ctx.out().line("__meta.setCustomModelData(" + ctx.java("val") + ");");
                    ctx.out().line(itemJava + ".setItemMeta(__meta);");
                    ctx.out().line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("make %i:ITEMSTACK% unbreakable")
                .description("Makes an item stack unbreakable.")
                .example("make item unbreakable")
                .since("1.0.0")
                .category(Categories.ITEM)
                .handler(ctx -> {
                    ctx.codegen().addImport(ITEM_META);
                    String itemJava = ctx.java("i");
                    ctx.out().line("{");
                    ctx.out().line("ItemMeta __meta = " + itemJava + ".getItemMeta();");
                    ctx.out().line("__meta.setUnbreakable(true);");
                    ctx.out().line(itemJava + ".setItemMeta(__meta);");
                    ctx.out().line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("make %i:ITEMSTACK% breakable")
                .description("Makes an item stack breakable again (removes unbreakable flag).")
                .example("make item breakable")
                .since("1.0.0")
                .category(Categories.ITEM)
                .handler(ctx -> {
                    ctx.codegen().addImport(ITEM_META);
                    String itemJava = ctx.java("i");
                    ctx.out().line("{");
                    ctx.out().line("ItemMeta __meta = " + itemJava + ".getItemMeta();");
                    ctx.out().line("__meta.setUnbreakable(false);");
                    ctx.out().line(itemJava + ".setItemMeta(__meta);");
                    ctx.out().line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %i:ITEMSTACK_POSSESSIVE% durability [to] %val:INT%")
                .description("Sets the durability damage of a damageable item.")
                .example("set item's durability to 100")
                .since("1.0.0")
                .category(Categories.ITEM)
                .handler(ctx -> {
                    ctx.codegen().addImport(DAMAGEABLE);
                    ctx.codegen().addImport(ITEM_META);
                    String itemJava = ctx.java("i");
                    ctx.out().line("{");
                    ctx.out().line("ItemMeta __meta = " + itemJava + ".getItemMeta();");
                    ctx.out().line("if (__meta instanceof Damageable __dmg) {");
                    ctx.out().line("__dmg.setDamage(" + ctx.java("val") + ");");
                    ctx.out().line(itemJava + ".setItemMeta(__meta);");
                    ctx.out().line("}");
                    ctx.out().line("}");
                }));
    }
}
