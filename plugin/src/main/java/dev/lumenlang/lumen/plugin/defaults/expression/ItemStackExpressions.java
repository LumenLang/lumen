package dev.lumenlang.lumen.plugin.defaults.expression;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.Types;
import dev.lumenlang.lumen.api.type.BuiltinLumenTypes;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Registers expression patterns for retrieving item stack properties: display name,
 * lore, amount, type, durability, custom model data, and creating new item stacks.
 */
@Registration
@SuppressWarnings("unused")
public final class ItemStackExpressions {

    private static final String DAMAGEABLE = Damageable.class.getName();
    private static final String ITEM_STACK = ItemStack.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen").pattern("new item %mat:MATERIAL%")
                .description("Creates a new ItemStack from a material name with an amount of 1.")
                .example("set sword to new item diamond_sword")
                .since("1.0.0").category(Categories.ITEM)
                .returnRefTypeId(MinecraftTypes.ITEMSTACK.id())
                .handler(ctx -> {
                    ctx.codegen().addImport(ITEM_STACK);
                    return new ExpressionResult(
                            "new ItemStack(" + ctx.java("mat") + ")",
                            MinecraftTypes.ITEMSTACK.id());
                }));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("new item %mat:MATERIAL% %amt:INT%")
                .description("Creates a new ItemStack from a material name with the specified amount.")
                .example("set swords to new item diamond_sword 5")
                .since("1.0.0").category(Categories.ITEM)
                .returnRefTypeId(MinecraftTypes.ITEMSTACK.id())
                .handler(ctx -> {
                    ctx.codegen().addImport(ITEM_STACK);
                    return new ExpressionResult(
                            "new ItemStack(" + ctx.java("mat") + ", " + ctx.java("amt") + ")",
                            MinecraftTypes.ITEMSTACK.id());
                }));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %i:ITEMSTACK_POSSESSIVE% (display name|name)")
                .description("Returns the display name of an item stack, or the material name if none is set.")
                .example("set name to get item's display name")
                .since("1.0.0").category(Categories.ITEM)
                .handler(ctx -> {
                    String java = ctx.java("i");
                    return new ExpressionResult(
                            "(" + java + ".hasItemMeta() && " + java + ".getItemMeta().hasDisplayName() ? " + java + ".getItemMeta().getDisplayName() : " + java + ".getType().name())",
                            null, Types.STRING);
                }));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %i:ITEMSTACK_POSSESSIVE% lore")
                .description("Returns the lore of an item stack as a list of strings.")
                .example("set lore to get item's lore")
                .since("1.0.0").category(Categories.ITEM)
                .returnRefTypeId(BuiltinLumenTypes.LIST.id())
                .handler(ctx -> {
                    ctx.codegen().addImport(List.class.getName());
                    ctx.codegen().addImport(Collections.class.getName());
                    String java = ctx.java("i");
                    return new ExpressionResult(
                            "(" + java + ".hasItemMeta() && " + java + ".getItemMeta().hasLore() ? " + java + ".getItemMeta().getLore() : Collections.<String>emptyList())",
                            BuiltinLumenTypes.LIST.id());
                }));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %i:ITEMSTACK_POSSESSIVE% amount")
                .description("Returns the stack amount of an item.")
                .example("set amt to get item's amount")
                .since("1.0.0").category(Categories.ITEM)
                .returnJavaType(Types.INT)
                .handler(ctx -> new ExpressionResult(ctx.java("i") + ".getAmount()", null, Types.INT)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %i:ITEMSTACK_POSSESSIVE% type")
                .description("Returns the material type name of an item stack.")
                .example("set mat to get item's type")
                .since("1.0.0").category(Categories.ITEM)
                .returnJavaType(Types.STRING)
                .handler(ctx -> new ExpressionResult(ctx.java("i") + ".getType().name()", null, Types.STRING)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %i:ITEMSTACK_POSSESSIVE% durability")
                .description("Returns the durability damage of a damageable item, or 0 if not damageable.")
                .example("set dmg to get item's durability")
                .since("1.0.0").category(Categories.ITEM)
                .returnJavaType(Types.INT)
                .handler(ctx -> {
                    ctx.codegen().addImport(DAMAGEABLE);
                    String java = ctx.java("i");
                    return new ExpressionResult(
                            "(" + java + ".getItemMeta() instanceof Damageable __dmg ? __dmg.getDamage() : 0)",
                            null, Types.INT);
                }));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %i:ITEMSTACK_POSSESSIVE% max durability")
                .description("Returns the maximum durability of the item's material.")
                .example("set maxDmg to get item's max durability")
                .since("1.0.0").category(Categories.ITEM)
                .returnJavaType(Types.INT)
                .handler(ctx -> new ExpressionResult(ctx.java("i") + ".getType().getMaxDurability()", null, Types.INT)));

        api.patterns().expression(b -> b
                .by("Lumen").pattern("get %i:ITEMSTACK_POSSESSIVE% custom model data")
                .description("Returns the custom model data of an item, or 0 if not set.")
                .example("set cmd to get item's custom model data")
                .since("1.0.0").category(Categories.ITEM)
                .returnJavaType(Types.INT)
                .handler(ctx -> {
                    String java = ctx.java("i");
                    return new ExpressionResult(
                            "(" + java + ".hasItemMeta() && " + java + ".getItemMeta().hasCustomModelData() ? " + java + ".getItemMeta().getCustomModelData() : 0)",
                            null, Types.INT);
                }));
    }
}
