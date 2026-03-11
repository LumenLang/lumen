package net.vansencool.lumen.plugin.defaults.condition;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.annotations.Call;
import net.vansencool.lumen.api.annotations.Registration;
import net.vansencool.lumen.plugin.util.InventoryHelper;
import net.vansencool.lumen.plugin.util.LumenInventoryHolder;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Registers all inventory-related condition patterns, including slot emptiness checks,
 * inventory emptiness, material containment, and Lumen inventory identification.
 */
@Registration
@SuppressWarnings("unused")
public final class InventoryConditions {

    private static final String INVENTORY = Inventory.class.getName();
    private static final String MATERIAL = Material.class.getName();

    @Call
    public void register(@NotNull LumenAPI api) {
        registerConditions();
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
}
