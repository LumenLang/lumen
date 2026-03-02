package net.vansencool.lumen.plugin.defaults.inventory;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.pattern.Categories;
import org.bukkit.block.Container;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Registers patterns for interacting with block container inventories
 * (chests, barrels, dispensers, hoppers, etc.).
 *
 * <p>All patterns guard against non-container blocks by checking whether
 * the block state implements {@code Container}.
 */
@Registration
@Description("Registers container inventory patterns: add item, check empty, clear")
@SuppressWarnings("unused")
public final class ContainerPatterns {

    private static final String CONTAINER = Container.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        registerStatements(api);
        registerConditions(api);
    }

    private void registerStatements(@NotNull LumenAPI api) {
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

    private void registerConditions(@NotNull LumenAPI api) {
        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%b:BLOCK% inventory is empty")
                .description("Checks if a container block's inventory is empty.")
                .example("if block inventory is empty:")
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .handler((match, env, ctx) -> {
                    ctx.addImport(CONTAINER);
                    String block = match.java("b", ctx, env);
                    return "(" + block + ".getState() instanceof Container __ct && __ct.getInventory().isEmpty())";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%b:BLOCK% inventory is not empty")
                .description("Checks if a container block's inventory is not empty.")
                .example("if block inventory is not empty:")
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .handler((match, env, ctx) -> {
                    ctx.addImport(CONTAINER);
                    String block = match.java("b", ctx, env);
                    return "(" + block + ".getState() instanceof Container __ct && !__ct.getInventory().isEmpty())";
                }));

        api.patterns().condition(b -> b
                .by("Lumen")
                .pattern("%b:BLOCK% is [a] container")
                .description("Checks if a block is a container (chest, barrel, hopper, etc.).")
                .example("if block is a container:")
                .since("1.0.0")
                .category(Categories.INVENTORY)
                .handler((match, env, ctx) -> {
                    ctx.addImport(CONTAINER);
                    String block = match.java("b", ctx, env);
                    return block + ".getState() instanceof Container";
                }));
    }
}
