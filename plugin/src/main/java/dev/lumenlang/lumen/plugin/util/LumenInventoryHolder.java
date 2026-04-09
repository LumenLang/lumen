package dev.lumenlang.lumen.plugin.util;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Custom {@link InventoryHolder} used by all inventories created through Lumen scripts.
 *
 * <p>Every Lumen-created inventory carries a name that uniquely identifies the
 * GUI type (e.g. {@code "settings"}). This allows scripts to
 * distinguish Lumen inventories from vanilla or third-party.
 *
 * <p>The name is not the display title. A GUI can have a colorful title shown
 * to the player while using a plain name for programmatic identification.
 */
public final class LumenInventoryHolder implements InventoryHolder {

    private final @NotNull String name;
    private @Nullable String title;
    private @Nullable Inventory inventory;

    /**
     * Creates a new holder with the given name.
     *
     * @param name the programmatic name of this inventory
     */
    public LumenInventoryHolder(@NotNull String name) {
        this.name = name;
    }

    /**
     * Returns the programmatic name of this inventory.
     *
     * @return the inventory name, never null
     */
    public @NotNull String name() {
        return name;
    }

    /**
     * Returns the display title of this inventory, or null if created without one.
     *
     * @return the display title, or null
     */
    public @Nullable String title() {
        return title;
    }

    /**
     * Assigns the display title for this inventory. Called internally
     * after creation so the hot reload system can detect title changes.
     *
     * @param title the display title
     */
    public void title(@NotNull String title) {
        this.title = title;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("Inventory has not been assigned yet");
        }
        return inventory;
    }

    /**
     * Assigns the actual inventory reference to this holder. Called internally
     * after {@code Bukkit.createInventory} is invoked.
     *
     * @param inventory the created inventory
     */
    public void inventory(@NotNull Inventory inventory) {
        this.inventory = inventory;
    }
}
