package net.vansencool.lumen.plugin.defaults.inventory;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.pattern.Categories;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Registers all built-in inventory-related statement patterns.
 */
@Registration
@Description("Registers inventory statements: give, clear inventory, close inventory")
@SuppressWarnings("unused")
public final class InventoryStatements {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("give %who:PLAYER% %item:MATERIAL% %amt:INT%")
                .description("Gives a player a specified amount of a material.")
                .example("give player diamond 64")
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(ItemStack.class.getName());
                    out.line(ctx.java("who") + ".getInventory().addItem(new ItemStack("
                            + ctx.java("item") + ", "
                            + ctx.java("amt") + "));");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("give %who:PLAYER% %item:ITEMSTACK%")
                .description("Gives a player an existing item stack variable.")
                .example("give player sword")
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .handler((line, ctx, out) -> out.line(ctx.java("who") + ".getInventory().addItem("
                        + ctx.java("item") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("give %who:PLAYER% %item:ITEMSTACK% %amt:INT%")
                .description("Gives a player an existing item stack variable with a specific amount.")
                .example("give player sword 3")
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(ItemStack.class.getName());
                    String itemJava = ctx.java("item");
                    out.line("{");
                    out.line("ItemStack __item = " + itemJava + ".clone();");
                    out.line("__item.setAmount(" + ctx.java("amt") + ");");
                    out.line(ctx.java("who") + ".getInventory().addItem(__item);");
                    out.line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("give %who:PLAYER% %item:ITEM%")
                .description("Gives a player an item stack.")
                .example("give player myItem")
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .handler((line, ctx, out) -> out.line(ctx.java("who") + ".getInventory().addItem("
                        + ctx.java("item") + ");")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("give %who:PLAYER% %item:ITEM% %amt:INT%")
                .description("Gives a player an item stack with a specific amount.")
                .example("give player myItem 5")
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(ItemStack.class.getName());
                    String itemJava = ctx.java("item");
                    out.line("{");
                    out.line("ItemStack __item = " + itemJava + ";");
                    out.line("__item.setAmount(" + ctx.java("amt") + ");");
                    out.line(ctx.java("who") + ".getInventory().addItem(__item);");
                    out.line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(clear|wipe) %who:PLAYER_POSSESSIVE% inventory")
                .description("Clears a player's entire inventory.")
                .example("clear player's inventory")
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .handler((line, ctx, out) -> out.line(ctx.java("who") + ".getInventory().clear();")));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("(close|shut) %who:PLAYER_POSSESSIVE% inventory")
                .description("Closes the player's currently open inventory screen.")
                .example("close player's inventory")
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .handler((line, ctx, out) -> out.line(ctx.java("who") + ".closeInventory();")));
    }
}
