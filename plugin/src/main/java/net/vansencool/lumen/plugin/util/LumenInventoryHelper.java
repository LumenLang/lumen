package net.vansencool.lumen.plugin.util;

import net.vansencool.lumen.plugin.text.LumenText;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Factory helper used by generated script code to create Lumen-managed inventories.
 *
 * <p>All methods attach a {@link LumenInventoryHolder} to the created inventory so
 * that click-event handlers can identify which GUI was interacted with.
 */
@SuppressWarnings("unused") // Called by generated code
public final class LumenInventoryHelper {

    private LumenInventoryHelper() {
    }

    /**
     * Creates a Lumen inventory with a fixed slot count and a colorized title.
     *
     * @param name  the programmatic name used to identify this GUI
     * @param size  the total number of slots (must be a multiple of 9)
     * @param title the display title, supports color codes
     * @return the created inventory
     */
    public static @NotNull Inventory create(@NotNull String name, int size, @NotNull String title) {
        if (size % 9 != 0) {
            throw new RuntimeException("Inventory size must be a multiple of 9, got " + size);
        }
        LumenInventoryHolder holder = new LumenInventoryHolder(name);
        Inventory inv = Bukkit.createInventory(holder, size, LumenText.colorize(title));
        holder.setInventory(inv);
        return inv;
    }

    /**
     * Creates a Lumen inventory with a fixed slot count and no title.
     *
     * @param name the programmatic name used to identify this GUI
     * @param size the total number of slots (must be a multiple of 9)
     * @return the created inventory
     */
    public static @NotNull Inventory create(@NotNull String name, int size) {
        if (size % 9 != 0) {
            throw new RuntimeException("Inventory size must be a multiple of 9, got " + size);
        }
        LumenInventoryHolder holder = new LumenInventoryHolder(name);
        Inventory inv = Bukkit.createInventory(holder, size);
        holder.setInventory(inv);
        return inv;
    }

    /**
     * Creates a Lumen inventory sized by row count with a colorized title.
     *
     * @param name  the programmatic name used to identify this GUI
     * @param rows  the number of rows (1 to 6)
     * @param title the display title, supports color codes
     * @return the created inventory
     */
    public static @NotNull Inventory createWithRows(@NotNull String name, int rows, @NotNull String title) {
        if (rows < 1 || rows > 6) {
            throw new RuntimeException("Rows must be between 1 and 6, got " + rows);
        }
        return create(name, rows * 9, title);
    }

    /**
     * Creates a Lumen inventory sized by row count with no title.
     *
     * @param name the programmatic name used to identify this GUI
     * @param rows the number of rows (1 to 6)
     * @return the created inventory
     */
    public static @NotNull Inventory createWithRows(@NotNull String name, int rows) {
        if (rows < 1 || rows > 6) {
            throw new RuntimeException("Rows must be between 1 and 6, got " + rows);
        }
        return create(name, rows * 9);
    }
}
