package net.vansencool.lumen.plugin.defaults.inventory;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Description;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.handler.ExpressionHandler.ExpressionResult;
import net.vansencool.lumen.api.type.RefTypes;
import net.vansencool.lumen.plugin.util.InventoryHelper;
import net.vansencool.lumen.plugin.util.InventoryRegistry;
import net.vansencool.lumen.plugin.util.LumenInventoryHelper;
import net.vansencool.lumen.plugin.util.LumenInventoryHolder;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Registers patterns for creating and manipulating custom inventories (GUIs).
 *
 * <p>This class provides the full inventory creation and interaction toolkit:
 * <ul>
 *   <li>Creating chest inventories with a title and size</li>
 *   <li>Setting, getting, and removing items in specific slots</li>
 *   <li>Opening and closing inventories for players</li>
 *   <li>Querying inventory properties (size, title, emptiness)</li>
 *   <li>Filling ranges of slots or clearing entire inventories</li>
 * </ul>
 *
 * <p>All inventories are standard Bukkit {@code Inventory} objects and can be opened
 * for any player. Combined with the {@code inventory_click} event, these patterns
 * enable fully dynamic GUI creation.
 */
@Registration
@Description("Registers custom inventory (GUI) patterns: create, set slot, get item, open, close, fill, clear")
@SuppressWarnings("unused")
public final class InventoryPatterns {

    private static final String INVENTORY = Inventory.class.getName();
    private static final String ITEM_STACK = ItemStack.class.getName();
    private static final String MATERIAL = Material.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        registerCreation();
        registerSlotManipulation();
        registerPlayerInteraction();
        registerConditions();
        registerExpressions();
    }

    private void registerCreation() {
        InventoryHelper.create()
                .expression(
                        "new inventory %name:STRING% [with] [size] %size:INT% titled %title:STRING%",
                        "Creates a new Lumen inventory with a name, size, and display title. The name identifies the GUI type programmatically.",
                        "var gui = new inventory \"main_menu\" with size 27 titled \"&6My Shop\"",
                        ctx -> {
                            ctx.codegen().addImport(INVENTORY);
                            ctx.codegen().addImport(LumenInventoryHelper.class.getName());
                            String name = ctx.java("name");
                            String size = ctx.java("size");
                            String title = ctx.java("title");
                            return new ExpressionResult(
                                    "LumenInventoryHelper.create(" + name + ", " + size + ", " + title + ")",
                                    null);
                        })
                .expression(
                        "new inventory %name:STRING% [with] [size] %size:INT%",
                        "Creates a new Lumen inventory with a name and size, without a display title.",
                        "var gui = new inventory \"shop\" with size 54",
                        ctx -> {
                            ctx.codegen().addImport(INVENTORY);
                            ctx.codegen().addImport(LumenInventoryHelper.class.getName());
                            String name = ctx.java("name");
                            String size = ctx.java("size");
                            return new ExpressionResult(
                                    "LumenInventoryHelper.create(" + name + ", " + size + ")",
                                    null);
                        })
                .expression(
                        "new inventory %name:STRING% [with] rows %rows:INT% titled %title:STRING%",
                        "Creates a new Lumen inventory with a name, row count (1 to 6), and display title. "
                                + "The size is calculated as rows * 9. Throws a runtime error if rows is not between 1 and 6.",
                        "var gui = new inventory \"main_menu\" with rows 3 titled \"&6My Shop\"",
                        ctx -> {
                            ctx.codegen().addImport(INVENTORY);
                            ctx.codegen().addImport(LumenInventoryHelper.class.getName());
                            String name = ctx.java("name");
                            String rows = ctx.java("rows");
                            String title = ctx.java("title");
                            return new ExpressionResult(
                                    "LumenInventoryHelper.createWithRows(" + name + ", " + rows + ", " + title + ")",
                                    null);
                        })
                .expression(
                        "new inventory %name:STRING% [with] rows %rows:INT%",
                        "Creates a new Lumen inventory with a name and row count (1 to 6), without a display title. "
                                + "The size is calculated as rows * 9. Throws a runtime error if rows is not between 1 and 6.",
                        "var gui = new inventory \"shop\" with rows 6",
                        ctx -> {
                            ctx.codegen().addImport(INVENTORY);
                            ctx.codegen().addImport(LumenInventoryHelper.class.getName());
                            String name = ctx.java("name");
                            String rows = ctx.java("rows");
                            return new ExpressionResult(
                                    "LumenInventoryHelper.createWithRows(" + name + ", " + rows + ")",
                                    null);
                        });
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
                        "open %inv:EXPR% for %who:PLAYER%",
                        "Opens a custom inventory for a player.",
                        "open gui for player",
                        (line, ctx, out) -> {
                            ctx.codegen().addImport(INVENTORY);
                            out.line(ctx.java("who") + ".openInventory((Inventory) " + ctx.java("inv") + ");");
                        })
                .statement(
                        "show %inv:EXPR% to %who:PLAYER%",
                        "Opens a custom inventory for a player (alias for 'open ... for').",
                        "show gui to player",
                        (line, ctx, out) -> {
                            ctx.codegen().addImport(INVENTORY);
                            out.line(ctx.java("who") + ".openInventory((Inventory) " + ctx.java("inv") + ");");
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

    private void registerConditions() {
        InventoryHelper.create()
                .conditionPair(
                        "slot %slot:INT% of %inv:EXPR% is empty",
                        "slot %slot:INT% of %inv:EXPR% is not empty",
                        "Checks if a specific slot in an inventory contains no item.",
                        "Checks if a specific slot in an inventory contains an item.",
                        "if slot 0 of gui is empty:",
                        "if slot 0 of gui is not empty:",
                        (match, env, ctx) -> {
                            ctx.addImport(INVENTORY);
                            ctx.addImport(MATERIAL);
                            String inv = match.java("inv", ctx, env);
                            String slot = match.java("slot", ctx, env);
                            return "(((Inventory) " + inv + ").getItem(" + slot + ") == null || "
                                    + "((Inventory) " + inv + ").getItem(" + slot + ").getType() == org.bukkit.Material.AIR)";
                        },
                        (match, env, ctx) -> {
                            ctx.addImport(INVENTORY);
                            ctx.addImport(MATERIAL);
                            String inv = match.java("inv", ctx, env);
                            String slot = match.java("slot", ctx, env);
                            return "(((Inventory) " + inv + ").getItem(" + slot + ") != null && "
                                    + "((Inventory) " + inv + ").getItem(" + slot + ").getType() != org.bukkit.Material.AIR)";
                        })
                .conditionPair(
                        "%inv:EXPR% inventory is empty",
                        "%inv:EXPR% inventory is not empty",
                        "Checks if a custom inventory is completely empty.",
                        "Checks if a custom inventory contains at least one item.",
                        "if gui inventory is empty:",
                        "if gui inventory is not empty:",
                        (match, env, ctx) -> {
                            ctx.addImport(INVENTORY);
                            return "((Inventory) " + match.java("inv", ctx, env) + ").isEmpty()";
                        },
                        (match, env, ctx) -> {
                            ctx.addImport(INVENTORY);
                            return "!((Inventory) " + match.java("inv", ctx, env) + ").isEmpty()";
                        })
                .condition(
                        "%inv:EXPR% inventory contains %mat:MATERIAL%",
                        "Checks if a custom inventory contains at least one item of the given material.",
                        "if gui inventory contains diamond:",
                        (match, env, ctx) -> {
                            ctx.addImport(INVENTORY);
                            return "((Inventory) " + match.java("inv", ctx, env)
                                    + ").contains(" + match.java("mat", ctx, env) + ")";
                        })
                .condition(
                        "%inv:EXPR% inventory does not contain %mat:MATERIAL%",
                        "Checks if a custom inventory does not contain any item of the given material.",
                        "if gui inventory does not contain diamond:",
                        (match, env, ctx) -> {
                            ctx.addImport(INVENTORY);
                            return "!((Inventory) " + match.java("inv", ctx, env)
                                    + ").contains(" + match.java("mat", ctx, env) + ")";
                        })
                .conditionPair(
                        "%inv:EXPR% is [a] lumen inventory",
                        "%inv:EXPR% is not [a] lumen inventory",
                        "Checks if the inventory was created by Lumen (uses a LumenInventoryHolder).",
                        "Checks if the inventory was not created by Lumen.",
                        "if inventory is a lumen inventory:",
                        "if inventory is not a lumen inventory:",
                        (match, env, ctx) -> {
                            ctx.addImport(INVENTORY);
                            ctx.addImport(LumenInventoryHolder.class.getName());
                            return "(((Inventory) " + match.java("inv", ctx, env)
                                    + ").getHolder() instanceof LumenInventoryHolder)";
                        },
                        (match, env, ctx) -> {
                            ctx.addImport(INVENTORY);
                            ctx.addImport(LumenInventoryHolder.class.getName());
                            return "(!(((Inventory) " + match.java("inv", ctx, env)
                                    + ").getHolder() instanceof LumenInventoryHolder))";
                        });
    }

    private void registerExpressions() {
        InventoryHelper.create()
                .expression(
                        "[get] item in slot %slot:INT% of %inv:EXPR%",
                        "Returns the item stack in a specific slot of an inventory, or null if the slot is empty.",
                        "var item = get item in slot 0 of gui",
                        ctx -> {
                            ctx.codegen().addImport(INVENTORY);
                            return new ExpressionResult(
                                    "((Inventory) " + ctx.java("inv") + ").getItem(" + ctx.java("slot") + ")",
                                    RefTypes.ITEMSTACK.id());
                        })
                .expression(
                        "[get] %inv:EXPR% inventory size",
                        "Returns the total number of slots in an inventory.",
                        "var sz = get gui inventory size",
                        ctx -> {
                            ctx.codegen().addImport(INVENTORY);
                            return new ExpressionResult(
                                    "((Inventory) " + ctx.java("inv") + ").getSize()",
                                    null);
                        })
                .expression(
                        "[get] first empty slot of %inv:EXPR%",
                        "Returns the index of the first empty slot in an inventory, or -1 if full.",
                        "var freeSlot = get first empty slot of gui",
                        ctx -> {
                            ctx.codegen().addImport(INVENTORY);
                            return new ExpressionResult(
                                    "((Inventory) " + ctx.java("inv") + ").firstEmpty()",
                                    null);
                        })
                .expression(
                        "[get] name of %inv:EXPR%",
                        "Returns the name of a Lumen inventory, or null if the inventory was not created by Lumen.",
                        "var gui_name = get name of inventory",
                        ctx -> {
                            ctx.codegen().addImport(INVENTORY);
                            ctx.codegen().addImport(LumenInventoryHolder.class.getName());
                            String inv = ctx.java("inv");
                            return new ExpressionResult(
                                    "(((Inventory) " + inv + ").getHolder() instanceof LumenInventoryHolder __lh ? __lh.getName() : null)",
                                    null);
                        });
    }
}
