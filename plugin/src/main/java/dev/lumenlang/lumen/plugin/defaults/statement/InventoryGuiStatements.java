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
// TODO: Add INVENTORY type binding and use it here.
// TODO: Cleanup this class
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
                        "set slot %slot:INT% of %inv:EXPR% to %item:ITEMSTACK%",
                        "Places an item stack in a specific slot of an inventory.",
                        "set slot 0 of gui to sword",
                        (line, ctx, out) -> {
                            ctx.codegen().addImport(INVENTORY);
                            out.line("((Inventory) " + ctx.java("inv") + ").setItem("
                                    + ctx.java("slot") + ", " + ctx.java("item") + ");");
                        })
                .statement(
                        "set slot %slot:INT% of %inv:EXPR% to %mat:MATERIAL%",
                        "Places a single item of the given material in a specific slot of an inventory.",
                        "set slot 4 of gui to diamond",
                        (line, ctx, out) -> {
                            ctx.codegen().addImport(INVENTORY);
                            ctx.codegen().addImport(ITEM_STACK);
                            out.line("((Inventory) " + ctx.java("inv") + ").setItem("
                                    + ctx.java("slot") + ", new ItemStack(" + ctx.java("mat") + "));");
                        })
                .statement(
                        "set slot %slot:INT% of %inv:EXPR% to %mat:MATERIAL% %amt:INT%",
                        "Places a stack of items in a specific slot of an inventory.",
                        "set slot 4 of gui to diamond 64",
                        (line, ctx, out) -> {
                            ctx.codegen().addImport(INVENTORY);
                            ctx.codegen().addImport(ITEM_STACK);
                            out.line("((Inventory) " + ctx.java("inv") + ").setItem("
                                    + ctx.java("slot") + ", new ItemStack(" + ctx.java("mat")
                                    + ", " + ctx.java("amt") + "));");
                        })
                .statement(
                        "(clear|remove) slot %slot:INT% of %inv:EXPR%",
                        "Removes the item from a specific slot of an inventory.",
                        "clear slot 0 of gui",
                        (line, ctx, out) -> {
                            ctx.codegen().addImport(INVENTORY);
                            out.line("((Inventory) " + ctx.java("inv") + ").clear(" + ctx.java("slot") + ");");
                        })
                .statement(
                        "(clear|wipe) %inv:EXPR% inventory",
                        "Removes all items from an inventory.",
                        "clear gui inventory",
                        (line, ctx, out) -> {
                            ctx.codegen().addImport(INVENTORY);
                            out.line("((Inventory) " + ctx.java("inv") + ").clear();");
                        })
                .statement(
                        "fill slots %from:INT% to %to:INT% of %inv:EXPR% with %item:ITEMSTACK%",
                        "Fills a range of slots in an inventory with the given item stack.",
                        "fill slots 0 to 8 of gui with filler",
                        (line, ctx, out) -> {
                            ctx.codegen().addImport(INVENTORY);
                            String inv = ctx.java("inv");
                            String from = ctx.java("from");
                            String to = ctx.java("to");
                            String item = ctx.java("item");
                            out.line("{");
                            out.line("Inventory __inv = (Inventory) " + inv + ";");
                            out.line("for (int __s = " + from + "; __s <= " + to + "; __s++) {");
                            out.line("    __inv.setItem(__s, " + item + ".clone());");
                            out.line("}");
                            out.line("}");
                        })
                .statement(
                        "fill slots %from:INT% to %to:INT% of %inv:EXPR% with %mat:MATERIAL%",
                        "Fills a range of slots in an inventory with the given material.",
                        "fill slots 0 to 8 of gui with gray_stained_glass_pane",
                        (line, ctx, out) -> {
                            ctx.codegen().addImport(INVENTORY);
                            ctx.codegen().addImport(ITEM_STACK);
                            String inv = ctx.java("inv");
                            String from = ctx.java("from");
                            String to = ctx.java("to");
                            String mat = ctx.java("mat");
                            out.line("{");
                            out.line("Inventory __inv = (Inventory) " + inv + ";");
                            out.line("for (int __s = " + from + "; __s <= " + to + "; __s++) {");
                            out.line("    __inv.setItem(__s, new ItemStack(" + mat + "));");
                            out.line("}");
                            out.line("}");
                        });
    }

    private void registerPlayerInteraction() {
        InventoryHelper.create()
                .statement(
                        "(show|open) %inv:EXPR% for %who:PLAYER%",
                        "Opens a custom inventory for a player.",
                        "open gui for player",
                        (line, ctx, out) -> {
                            ctx.codegen().addImport(INVENTORY);
                            ctx.codegen().addImport(InventoryHotReload.class.getName());
                            out.line("InventoryHotReload.openOrReplace(" + ctx.java("who") + ", (Inventory) " + ctx.java("inv") + ");");
                        })
                .statement(
                        "open inventory [named] %name:STRING% for %who:PLAYER%",
                        "Invokes a registered inventory builder by name for a player. The builder must have been registered via a 'register inventory' block.",
                        "open inventory named \"shop\" for player",
                        (line, ctx, out) -> {
                            ctx.codegen().addImport(InventoryRegistry.class.getName());
                            out.line("InventoryRegistry.open(" + ctx.java("name") + ", " + ctx.java("who") + ");");
                        });
    }
}
