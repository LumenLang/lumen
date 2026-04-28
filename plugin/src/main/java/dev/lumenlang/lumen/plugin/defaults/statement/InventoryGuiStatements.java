package dev.lumenlang.lumen.plugin.defaults.statement;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.plugin.util.InventoryHelper;
import dev.lumenlang.lumen.plugin.util.InventoryHotReload;
import dev.lumenlang.lumen.plugin.util.InventoryRegistry;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Registers all inventory GUI statement patterns for slot manipulation
 * and player interaction with custom inventories.
 */
@Registration
@SuppressWarnings("unused")
public final class InventoryGuiStatements {

    private static final String INVENTORY = Inventory.class.getName();
    private static final String ITEM_STACK = ItemStack.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        registerSlotManipulation();
        registerPlayerInteraction();
    }

    private void registerSlotManipulation() {
        InventoryHelper.create()
                .statement(
                        "set slot %slot:INT% of %inv:INVENTORY% to %item:ITEMSTACK%",
                        "Places an item stack in a specific slot of an inventory.",
                        "set slot 0 of gui to sword",
                        ctx -> ctx.out().line(ctx.java("inv") + ".setItem("
                                + ctx.java("slot") + ", " + ctx.java("item") + ");"))
                .statement(
                        "set slot %slot:INT% of %inv:INVENTORY% to %mat:MATERIAL%",
                        "Places a single item of the given material in a specific slot of an inventory.",
                        "set slot 4 of gui to diamond",
                        ctx -> {
                            ctx.codegen().addImport(ITEM_STACK);
                            ctx.out().line(ctx.java("inv") + ".setItem("
                                    + ctx.java("slot") + ", new ItemStack(" + ctx.java("mat") + "));");
                        })
                .statement(
                        "set slot %slot:INT% of %inv:INVENTORY% to %mat:MATERIAL% %amt:INT%",
                        "Places a stack of items in a specific slot of an inventory.",
                        "set slot 4 of gui to diamond 64",
                        ctx -> {
                            ctx.codegen().addImport(ITEM_STACK);
                            ctx.out().line(ctx.java("inv") + ".setItem("
                                    + ctx.java("slot") + ", new ItemStack(" + ctx.java("mat")
                                    + ", " + ctx.java("amt") + "));");
                        })
                .statement(
                        "(clear|remove) slot %slot:INT% of %inv:INVENTORY%",
                        "Removes the item from a specific slot of an inventory.",
                        "clear slot 0 of gui",
                        ctx -> ctx.out().line(ctx.java("inv") + ".clear(" + ctx.java("slot") + ");"))
                .statement(
                        "(clear|wipe) %inv:INVENTORY% inventory",
                        "Removes all items from an inventory.",
                        "clear gui inventory",
                        ctx -> ctx.out().line(ctx.java("inv") + ".clear();"))
                .statement(
                        "fill slots %from:INT% to %to:INT% of %inv:INVENTORY% with %item:ITEMSTACK%",
                        "Fills a range of slots in an inventory with the given item stack.",
                        "fill slots 0 to 8 of gui with filler",
                        ctx -> {
                            ctx.codegen().addImport(INVENTORY);
                            String inv = ctx.java("inv");
                            String from = ctx.java("from");
                            String to = ctx.java("to");
                            String item = ctx.java("item");
                            ctx.out().line("{");
                            ctx.out().line("Inventory __inv = " + inv + ";");
                            ctx.out().line("for (int __s = " + from + "; __s <= " + to + "; __s++) {");
                            ctx.out().line("    __inv.setItem(__s, " + item + ".clone());");
                            ctx.out().line("}");
                            ctx.out().line("}");
                        })
                .statement(
                        "fill slots %from:INT% to %to:INT% of %inv:INVENTORY% with %mat:MATERIAL%",
                        "Fills a range of slots in an inventory with the given material.",
                        "fill slots 0 to 8 of gui with gray_stained_glass_pane",
                        ctx -> {
                            ctx.codegen().addImport(ITEM_STACK);
                            String inv = ctx.java("inv");
                            String from = ctx.java("from");
                            String to = ctx.java("to");
                            String mat = ctx.java("mat");
                            ctx.out().line("{");
                            ctx.out().line("Inventory __inv = " + inv + ";");
                            ctx.out().line("for (int __s = " + from + "; __s <= " + to + "; __s++) {");
                            ctx.out().line("    __inv.setItem(__s, new ItemStack(" + mat + "));");
                            ctx.out().line("}");
                            ctx.out().line("}");
                        });
    }

    private void registerPlayerInteraction() {
        InventoryHelper.create()
                .statement(
                        "(show|open) %inv:INVENTORY% for %who:PLAYER%",
                        "Opens a custom inventory for a player.",
                        "open gui for player",
                        ctx -> {
                            ctx.codegen().addImport(InventoryHotReload.class.getName());
                            ctx.out().line("InventoryHotReload.openOrReplace(" + ctx.java("who") + ", " + ctx.java("inv") + ");");
                        })
                .statement(
                        "open inventory [named] %name:STRING% for %who:PLAYER%",
                        "Invokes a registered inventory builder by name for a player. The builder must have been registered via a 'register inventory' block.",
                        "open inventory named \"shop\" for player",
                        ctx -> {
                            ctx.codegen().addImport(InventoryRegistry.class.getName());
                            ctx.out().line("InventoryRegistry.open(" + ctx.java("name") + ", " + ctx.java("who") + ");");
                        });
    }
}
