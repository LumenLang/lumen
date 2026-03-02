package net.vansencool.lumen.plugin.defaults.inventory;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.handler.ExpressionHandler.ExpressionResult;
import net.vansencool.lumen.api.pattern.Categories;
import net.vansencool.lumen.api.type.RefTypes;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Registration
@Description("Registers ItemStack manipulation patterns: display name, lore, amount, type, enchantments, durability, and custom model data.")
@SuppressWarnings("unused")
public final class ItemStackPatterns {

    private static final String ITEM_META = ItemMeta.class.getName();
    private static final String DAMAGEABLE = Damageable.class.getName();
    private static final String ITEM_STACK = ItemStack.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        registerStatements(api);
        registerConditions(api);
        registerExpressions(api);
    }

    private void registerStatements(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %i:ITEMSTACK_POSSESSIVE% (display name|name) [to] %name:STRING%")
                .description("Sets the display name of an item stack. Supports color codes via LumenText.")
                .example("set item's display name to \"&6Golden Sword\"")
                .since("1.0.0")
                .category(Categories.ITEM)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(ITEM_META);
                    String itemJava = ctx.java("i");
                    out.line("{");
                    out.line("ItemMeta __meta = " + itemJava + ".getItemMeta();");
                    out.line("__meta.setDisplayName(LumenText.colorize(" + ctx.java("name") + "));");
                    out.line(itemJava + ".setItemMeta(__meta);");
                    out.line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %i:ITEMSTACK_POSSESSIVE% lore [to] %lore:STRING%")
                .description("Sets the lore of an item stack. Use | to separate lines. Supports color codes via LumenText.")
                .example("set item's lore to \"&7A legendary weapon|&eCrafted by the gods\"")
                .since("1.0.0")
                .category(Categories.ITEM)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(ITEM_META);
                    ctx.codegen().addImport(Arrays.class.getName());
                    ctx.codegen().addImport(Collectors.class.getName());
                    String itemJava = ctx.java("i");
                    out.line("{");
                    out.line("ItemMeta __meta = " + itemJava + ".getItemMeta();");
                    out.line("__meta.setLore(Arrays.stream((" + ctx.java("lore") + ").split(\"\\\\|\")).map(LumenText::colorize).collect(Collectors.toList()));"  );
                    out.line(itemJava + ".setItemMeta(__meta);");
                    out.line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("add lore %line:STRING% to %i:ITEMSTACK%")
                .description("Appends a single lore line to an item stack. Supports color codes via LumenText.")
                .example("add lore \"&aThis item is special\" to item")
                .since("1.0.0")
                .category(Categories.ITEM)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(ITEM_META);
                    ctx.codegen().addImport(List.class.getName());
                    ctx.codegen().addImport(ArrayList.class.getName());
                    String itemJava = ctx.java("i");
                    out.line("{");
                    out.line("ItemMeta __meta = " + itemJava + ".getItemMeta();");
                    out.line("List<String> __lore = __meta.hasLore() ? new ArrayList<>(__meta.getLore()) : new ArrayList<>();");
                    out.line("__lore.add(LumenText.colorize(" + ctx.java("line") + "));");
                    out.line("__meta.setLore(__lore);");
                    out.line(itemJava + ".setItemMeta(__meta);");
                    out.line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(clear|remove|wipe) %i:ITEMSTACK_POSSESSIVE% lore")
                .description("Removes all lore from an item stack.")
                .example("clear item's lore")
                .since("1.0.0")
                .category(Categories.ITEM)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(ITEM_META);
                    String itemJava = ctx.java("i");
                    out.line("{");
                    out.line("ItemMeta __meta = " + itemJava + ".getItemMeta();");
                    out.line("__meta.setLore(null);");
                    out.line(itemJava + ".setItemMeta(__meta);");
                    out.line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %i:ITEMSTACK_POSSESSIVE% amount [to] %amt:INT%")
                .description("Sets the stack amount of an item.")
                .example("set item's amount to 32")
                .since("1.0.0")
                .category(Categories.ITEM)
                .handler((line, ctx, out) ->
                        out.line(ctx.java("i") + ".setAmount(" + ctx.java("amt") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %i:ITEMSTACK_POSSESSIVE% type [to] %mat:MATERIAL%")
                .description("Changes the material type of an item stack.")
                .example("set item's type to diamond_sword")
                .since("1.0.0")
                .category(Categories.ITEM)
                .handler((line, ctx, out) ->
                        out.line(ctx.java("i") + ".setType(" + ctx.java("mat") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %i:ITEMSTACK_POSSESSIVE% custom model data [to] %val:INT%")
                .description("Sets the custom model data of an item stack.")
                .example("set item's custom model data to 1001")
                .since("1.0.0")
                .category(Categories.ITEM)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(ITEM_META);
                    String itemJava = ctx.java("i");
                    out.line("{");
                    out.line("ItemMeta __meta = " + itemJava + ".getItemMeta();");
                    out.line("__meta.setCustomModelData(" + ctx.java("val") + ");");
                    out.line(itemJava + ".setItemMeta(__meta);");
                    out.line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("make %i:ITEMSTACK% unbreakable")
                .description("Makes an item stack unbreakable.")
                .example("make item unbreakable")
                .since("1.0.0")
                .category(Categories.ITEM)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(ITEM_META);
                    String itemJava = ctx.java("i");
                    out.line("{");
                    out.line("ItemMeta __meta = " + itemJava + ".getItemMeta();");
                    out.line("__meta.setUnbreakable(true);");
                    out.line(itemJava + ".setItemMeta(__meta);");
                    out.line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("make %i:ITEMSTACK% breakable")
                .description("Makes an item stack breakable again (removes unbreakable flag).")
                .example("make item breakable")
                .since("1.0.0")
                .category(Categories.ITEM)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(ITEM_META);
                    String itemJava = ctx.java("i");
                    out.line("{");
                    out.line("ItemMeta __meta = " + itemJava + ".getItemMeta();");
                    out.line("__meta.setUnbreakable(false);");
                    out.line(itemJava + ".setItemMeta(__meta);");
                    out.line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %i:ITEMSTACK_POSSESSIVE% durability [to] %val:INT%")
                .description("Sets the durability damage of a damageable item.")
                .example("set item's durability to 100")
                .since("1.0.0")
                .category(Categories.ITEM)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(DAMAGEABLE);
                    ctx.codegen().addImport(ITEM_META);
                    String itemJava = ctx.java("i");
                    out.line("{");
                    out.line("ItemMeta __meta = " + itemJava + ".getItemMeta();");
                    out.line("if (__meta instanceof Damageable __dmg) {");
                    out.line("__dmg.setDamage(" + ctx.java("val") + ");");
                    out.line(itemJava + ".setItemMeta(__meta);");
                    out.line("}");
                    out.line("}");
                }));
    }

    private void registerConditions(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen").pattern("%i:ITEMSTACK% has (display name|name)")
                .description("Checks if an item stack has a custom display name.")
                .example("if item has display name:")
                .since("1.0.0").category(Categories.ITEM)
                .handler((match, env, ctx) ->
                        "(" + match.ref("i").java() + ".hasItemMeta() && " + match.ref("i").java() + ".getItemMeta().hasDisplayName())"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%i:ITEMSTACK% has lore")
                .description("Checks if an item stack has lore set.")
                .example("if item has lore:")
                .since("1.0.0").category(Categories.ITEM)
                .handler((match, env, ctx) ->
                        "(" + match.ref("i").java() + ".hasItemMeta() && " + match.ref("i").java() + ".getItemMeta().hasLore())"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%i:ITEMSTACK% has enchantments")
                .description("Checks if an item stack has any enchantments.")
                .example("if item has enchantments:")
                .since("1.0.0").category(Categories.ITEM)
                .handler((match, env, ctx) ->
                        "!" + match.ref("i").java() + ".getEnchantments().isEmpty()"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%i:ITEMSTACK% (does not have|has no) (display name|name)")
                .description("Checks if an item stack does not have a custom display name.")
                .example("if item does not have display name:")
                .since("1.0.0").category(Categories.ITEM)
                .handler((match, env, ctx) ->
                        "(!(" + match.ref("i").java() + ".hasItemMeta() && " + match.ref("i").java() + ".getItemMeta().hasDisplayName()))"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%i:ITEMSTACK% (does not have|has no) lore")
                .description("Checks if an item stack does not have lore.")
                .example("if item does not have lore:")
                .since("1.0.0").category(Categories.ITEM)
                .handler((match, env, ctx) ->
                        "(!(" + match.ref("i").java() + ".hasItemMeta() && " + match.ref("i").java() + ".getItemMeta().hasLore()))"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%i:ITEMSTACK% is unbreakable")
                .description("Checks if an item stack is unbreakable.")
                .example("if item is unbreakable:")
                .since("1.0.0").category(Categories.ITEM)
                .handler((match, env, ctx) ->
                        "(" + match.ref("i").java() + ".hasItemMeta() && " + match.ref("i").java() + ".getItemMeta().isUnbreakable())"));

        api.patterns().condition(b -> b
                .by("Lumen").pattern("%i:ITEMSTACK% is not unbreakable")
                .description("Checks if an item stack is not unbreakable.")
                .example("if item is not unbreakable:")
                .since("1.0.0").category(Categories.ITEM)
                .handler((match, env, ctx) ->
                        "(!(" + match.ref("i").java() + ".hasItemMeta() && " + match.ref("i").java() + ".getItemMeta().isUnbreakable()))"));
    }

    private void registerExpressions(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen").pattern("new item %mat:MATERIAL%")
                .description("Creates a new ItemStack from a material name with an amount of 1.")
                .example("var sword = new item diamond_sword")
                .since("1.0.0").category(Categories.ITEM)
                .handler(ctx -> {
                    ctx.codegen().addImport(ITEM_STACK);
                    return new ExpressionResult(
                            "new ItemStack(" + ctx.java("mat") + ")",
                            RefTypes.ITEMSTACK.id());
                }));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("new item %mat:MATERIAL% %amt:INT%")
                .description("Creates a new ItemStack from a material name with the specified amount.")
                .example("var swords = new item diamond_sword 5")
                .since("1.0.0").category(Categories.ITEM)
                .handler(ctx -> {
                    ctx.codegen().addImport(ITEM_STACK);
                    return new ExpressionResult(
                            "new ItemStack(" + ctx.java("mat") + ", " + ctx.java("amt") + ")",
                            RefTypes.ITEMSTACK.id());
                }));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %i:ITEMSTACK_POSSESSIVE% (display name|name)")
                .description("Returns the display name of an item stack, or the material name if none is set.")
                .example("var name = item's display name")
                .since("1.0.0").category(Categories.ITEM)
                .handler(ctx -> {
                    String java = ctx.java("i");
                    return new ExpressionResult(
                            "(" + java + ".hasItemMeta() && " + java + ".getItemMeta().hasDisplayName() ? " + java + ".getItemMeta().getDisplayName() : " + java + ".getType().name())",
                            null);
                }));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %i:ITEMSTACK_POSSESSIVE% lore")
                .description("Returns the lore of an item stack as a list of strings.")
                .example("var lore = item's lore")
                .since("1.0.0").category(Categories.ITEM)
                .handler(ctx -> {
                    ctx.codegen().addImport(List.class.getName());
                    ctx.codegen().addImport(Collections.class.getName());
                    String java = ctx.java("i");
                    return new ExpressionResult(
                            "(" + java + ".hasItemMeta() && " + java + ".getItemMeta().hasLore() ? " + java + ".getItemMeta().getLore() : Collections.<String>emptyList())",
                            RefTypes.LIST.id());
                }));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %i:ITEMSTACK_POSSESSIVE% amount")
                .description("Returns the stack amount of an item.")
                .example("var amt = item's amount")
                .since("1.0.0").category(Categories.ITEM)
                .handler(ctx -> new ExpressionResult(ctx.java("i") + ".getAmount()", null)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %i:ITEMSTACK_POSSESSIVE% type")
                .description("Returns the material type name of an item stack.")
                .example("var mat = item's type")
                .since("1.0.0").category(Categories.ITEM)
                .handler(ctx -> new ExpressionResult(ctx.java("i") + ".getType().name()", null)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %i:ITEMSTACK_POSSESSIVE% durability")
                .description("Returns the durability damage of a damageable item, or 0 if not damageable.")
                .example("var dmg = item's durability")
                .since("1.0.0").category(Categories.ITEM)
                .handler(ctx -> {
                    ctx.codegen().addImport(DAMAGEABLE);
                    String java = ctx.java("i");
                    return new ExpressionResult(
                            "(" + java + ".getItemMeta() instanceof Damageable __dmg ? __dmg.getDamage() : 0)",
                            null);
                }));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %i:ITEMSTACK_POSSESSIVE% max durability")
                .description("Returns the maximum durability of the item's material.")
                .example("var maxDmg = item's max durability")
                .since("1.0.0").category(Categories.ITEM)
                .handler(ctx -> new ExpressionResult(ctx.java("i") + ".getType().getMaxDurability()", null)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %i:ITEMSTACK_POSSESSIVE% custom model data")
                .description("Returns the custom model data of an item, or 0 if not set.")
                .example("var cmd = item's custom model data")
                .since("1.0.0").category(Categories.ITEM)
                .handler(ctx -> {
                    String java = ctx.java("i");
                    return new ExpressionResult(
                            "(" + java + ".hasItemMeta() && " + java + ".getItemMeta().hasCustomModelData() ? " + java + ".getItemMeta().getCustomModelData() : 0)",
                            null);
                }));
    }
}
