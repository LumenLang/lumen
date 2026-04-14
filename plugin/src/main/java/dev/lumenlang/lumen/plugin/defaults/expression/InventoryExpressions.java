package dev.lumenlang.lumen.plugin.defaults.expression;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.type.MinecraftTypes;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.plugin.util.InventoryHelper;
import dev.lumenlang.lumen.plugin.util.LumenInventoryHelper;
import dev.lumenlang.lumen.plugin.util.LumenInventoryHolder;
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
                        "set gui to new inventory \"main_menu\" with size 27 titled \"<gold>Test Menu\"",
                        ctx -> {
                            ctx.codegen().addImport(INVENTORY);
                            ctx.codegen().addImport(LumenInventoryHelper.class.getName());
                            String name = ctx.java("name");
                            String size = ctx.java("size");
                            String title = ctx.java("title");
                            return new ExpressionResult(
                                    "LumenInventoryHelper.create(" + name + ", " + size + ", " + title + ")",
                                    MinecraftTypes.INVENTORY);
                        })
                .expression(
                        "new inventory %name:STRING% [with] [size] %size:INT%",
                        "Creates a new Lumen inventory with a name and size, without a display title.",
                        "set gui to new inventory \"shop\" with size 54",
                        ctx -> {
                            ctx.codegen().addImport(INVENTORY);
                            ctx.codegen().addImport(LumenInventoryHelper.class.getName());
                            String name = ctx.java("name");
                            String size = ctx.java("size");
                            return new ExpressionResult(
                                    "LumenInventoryHelper.create(" + name + ", " + size + ")",
                                    MinecraftTypes.INVENTORY);
                        })
                .expression(
                        "new inventory %name:STRING% [with] rows %rows:INT% titled %title:STRING%",
                        "Creates a new Lumen inventory with a name, row count (1 to 6), and display title. "
                                + "The size is calculated as rows * 9. Throws a runtime error if rows is not between 1 and 6.",
                        "set gui to new inventory \"main_menu\" with rows 3 titled \"<gold>Test Menu\"",
                        ctx -> {
                            ctx.codegen().addImport(INVENTORY);
                            ctx.codegen().addImport(LumenInventoryHelper.class.getName());
                            String name = ctx.java("name");
                            String rows = ctx.java("rows");
                            String title = ctx.java("title");
                            return new ExpressionResult(
                                    "LumenInventoryHelper.createWithRows(" + name + ", " + rows + ", " + title + ")",
                                    MinecraftTypes.INVENTORY);
                        })
                .expression(
                        "new inventory %name:STRING% [with] rows %rows:INT%",
                        "Creates a new Lumen inventory with a name and row count (1 to 6), without a display title. "
                                + "The size is calculated as rows * 9. Throws a runtime error if rows is not between 1 and 6.",
                        "set gui to new inventory \"shop\" with rows 6",
                        ctx -> {
                            ctx.codegen().addImport(INVENTORY);
                            ctx.codegen().addImport(LumenInventoryHelper.class.getName());
                            String name = ctx.java("name");
                            String rows = ctx.java("rows");
                            return new ExpressionResult(
                                    "LumenInventoryHelper.createWithRows(" + name + ", " + rows + ")",
                                    MinecraftTypes.INVENTORY);
                        });
    }

    private void registerExpressions() {
        InventoryHelper.create()
                .expression(
                        "[get] item in slot %slot:INT% of %inv:INVENTORY%",
                        "Returns the item stack in a specific slot of an inventory, or null if the slot is empty.",
                        "set item to get item in slot 0 of gui",
                        ctx -> {
                            return new ExpressionResult(
                                    ctx.java("inv") + ".getItem(" + ctx.java("slot") + ")",
                                    MinecraftTypes.ITEMSTACK);
                        })
                .expression(
                        "[get] %inv:INVENTORY% inventory size",
                        "Returns the total number of slots in an inventory.",
                        "set sz to get gui inventory size",
                        ctx -> {
                            return new ExpressionResult(
                                    ctx.java("inv") + ".getSize()",
                                    PrimitiveType.INT);
                        })
                .expression(
                        "[get] first empty slot of %inv:INVENTORY%",
                        "Returns the index of the first empty slot in an inventory, or -1 if full.",
                        "set freeSlot to get first empty slot of gui",
                        ctx -> {
                            return new ExpressionResult(
                                    ctx.java("inv") + ".firstEmpty()",
                                    PrimitiveType.INT);
                        })
                .expression(
                        "[get] name of %inv:INVENTORY%",
                        "Returns the name of a Lumen inventory, or null if the inventory was not created by Lumen.",
                        "set gui_name to get name of inventory",
                        ctx -> {
                            ctx.codegen().addImport(LumenInventoryHolder.class.getName());
                            String inv = ctx.java("inv");
                            return new ExpressionResult(
                                    "(" + inv + ".getHolder() instanceof LumenInventoryHolder __lh ? __lh.name() : null)",
                                    PrimitiveType.STRING);
                        });
    }
}
