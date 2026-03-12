package net.vansencool.lumen.plugin.defaults.expression;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.api.handler.ExpressionHandler.ExpressionResult;
import net.vansencool.lumen.api.type.Types;
import net.vansencool.lumen.plugin.util.InventoryHelper;
import net.vansencool.lumen.plugin.util.LumenInventoryHelper;
import net.vansencool.lumen.plugin.util.LumenInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Registers all inventory-related expression patterns, including inventory creation
 * and inventory property queries.
 */
@Registration
@SuppressWarnings("unused")
public final class InventoryExpressions {

    private static final String INVENTORY = Inventory.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        registerCreation();
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
                                    Types.ITEMSTACK.id());
                        })
                .expression(
                        "[get] %inv:EXPR% inventory size",
                        "Returns the total number of slots in an inventory.",
                        "var sz = get gui inventory size",
                        ctx -> {
                            ctx.codegen().addImport(INVENTORY);
                            return new ExpressionResult(
                                    "((Inventory) " + ctx.java("inv") + ").getSize()",
                                    null, Types.INT);
                        })
                .expression(
                        "[get] first empty slot of %inv:EXPR%",
                        "Returns the index of the first empty slot in an inventory, or -1 if full.",
                        "var freeSlot = get first empty slot of gui",
                        ctx -> {
                            ctx.codegen().addImport(INVENTORY);
                            return new ExpressionResult(
                                    "((Inventory) " + ctx.java("inv") + ").firstEmpty()",
                                    null, Types.INT);
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
                                    null, Types.STRING);
                        });
    }
}
