package net.vansencool.lumen.plugin.defaults.statement;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.pattern.Categories;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Registers statement patterns for interacting with block container inventories
 * (chests, barrels, dispensers, hoppers, etc.).
 */
@Registration
@SuppressWarnings("unused")
public final class ContainerStatements {

    private static final String CONTAINER = Container.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("add %item:MATERIAL% %amt:INT% to %b:BLOCK% inventory")
                .description("Adds the specified amount of a material to a container block's inventory.")
                .example("add diamond 3 to block inventory")
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(ItemStack.class.getName());
                    ctx.codegen().addImport(CONTAINER);
                    String block = ctx.java("b");
                    String mat = ctx.java("item");
                    String amt = ctx.java("amt");
                    out.line("if (" + block + ".getState() instanceof Container __container) {");
                    out.line("    __container.getInventory().addItem(new ItemStack(" + mat + ", " + amt + "));");
                    out.line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("add %item:ITEMSTACK% to %b:BLOCK% inventory")
                .description("Adds an item stack to a container block's inventory.")
                .example("add myItem to block inventory")
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(CONTAINER);
                    String block = ctx.java("b");
                    String item = ctx.java("item");
                    out.line("if (" + block + ".getState() instanceof Container __container) {");
                    out.line("    __container.getInventory().addItem(" + item + ");");
                    out.line("}");
                }));

        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("clear %b:BLOCK% inventory")
                .description("Clears all items from a container block's inventory.")
                .example("clear block inventory")
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .handler((line, ctx, out) -> {
                    ctx.codegen().addImport(CONTAINER);
                    String block = ctx.java("b");
                    out.line("if (" + block + ".getState() instanceof Container __container) {");
                    out.line("    __container.getInventory().clear();");
                    out.line("}");
                }));
    }
}
