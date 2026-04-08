package dev.lumenlang.lumen.plugin.util;

import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.plugin.Lumen;
import dev.lumenlang.lumen.plugin.configuration.LumenConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles seamless in place inventory updates when inventory hot reload feature is enabled.
 *
 * <p>When a script opens an inventory for a player who already has a
 * Lumen inventory with the same programmatic name open, this class
 * replaces the contents and title in place rather than closing and
 * reopening the GUI. This eliminates the need to reopen the GUI.
 *
 * <p>When a registered inventory is opened via {@link InventoryRegistry},
 * the viewer is tracked so that script reloads can automatically refresh
 * all players who have that inventory open.
 */
@SuppressWarnings("unused")
public final class InventoryHotReload {

    private static final ConcurrentHashMap<UUID, String> VIEWERS = new ConcurrentHashMap<>();
    private static final AtomicBoolean LISTENER_REGISTERED = new AtomicBoolean(false);

    private InventoryHotReload() {
    }

    /**
     * Opens an inventory for a player, or replaces the currently open inventory
     * in place if the player already has a Lumen inventory with the same name open
     * and inventory hot reload is enabled.
     *
     * <p>When replacement occurs:
     * <ul>
     *   <li>If both inventories have the same size, items are copied in place
     *       and the title is updated.</li>
     *   <li>If the sizes differ, a normal open is performed since the underlying
     *       Bukkit inventory cannot be resized.</li>
     * </ul>
     *
     * @param player       the target player
     * @param newInventory the inventory to open or replace with
     */
    public static void openOrReplace(@NotNull Player player, @NotNull Inventory newInventory) {
        if (!LumenConfiguration.FEATURES.INVENTORIES.HOT_RELOAD) {
            player.openInventory(newInventory);
            return;
        }

        if (!(newInventory.getHolder() instanceof LumenInventoryHolder newHolder)) {
            player.openInventory(newInventory);
            return;
        }

        InventoryView view = player.getOpenInventory();
        Inventory currentInventory = view.getTopInventory();

        if (!(currentInventory.getHolder() instanceof LumenInventoryHolder currentHolder)) {
            player.openInventory(newInventory);
            return;
        }

        if (!currentHolder.name().equals(newHolder.name())) {
            player.openInventory(newInventory);
            return;
        }

        if (currentInventory.getSize() != newInventory.getSize()) {
            player.openInventory(newInventory);
            return;
        }

        replaceContents(currentInventory, newInventory);
        replaceTitle(view, newHolder);
    }

    /**
     * Records that a player is viewing a registry managed inventory.
     * Called by {@link InventoryRegistry} after a builder method completes.
     * Lazily registers the close listener the first time a viewer is tracked.
     *
     * @param playerId     the player's unique id
     * @param registryName the registry name of the opened inventory
     */
    public static void trackViewer(@NotNull UUID playerId, @NotNull String registryName) {
        if (!LumenConfiguration.FEATURES.INVENTORIES.HOT_RELOAD) return;
        ensureListenerRegistered();
        VIEWERS.put(playerId, registryName);
    }

    /**
     * Refreshes all tracked viewers by re invoking the registered inventory
     * builders. Players whose inventory is no longer a Lumen inventory or
     * whose builder is no longer registered are silently removed from tracking.
     *
     * <p>This should be called on the main thread after a script reload completes.
     */
    public static void refreshAll() {
        if (!LumenConfiguration.FEATURES.INVENTORIES.HOT_RELOAD) return;
        if (VIEWERS.isEmpty()) return;
        int refreshed = 0;
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : VIEWERS.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                toRemove.add(entry.getKey());
                continue;
            }
            InventoryView view = player.getOpenInventory();
            if (!(view.getTopInventory().getHolder() instanceof LumenInventoryHolder)) {
                toRemove.add(entry.getKey());
                continue;
            }
            String registryName = entry.getValue();
            if (!InventoryRegistry.isRegistered(registryName)) {
                toRemove.add(entry.getKey());
                continue;
            }
            try {
                InventoryRegistry.open(registryName, player);
                refreshed++;
            } catch (Throwable e) {
                LumenLogger.warning("[InventoryHotReload] Failed to refresh inventory '" + registryName + "' for " + player.getName() + ": " + e.getMessage());
            }
        }
        toRemove.forEach(VIEWERS::remove);
        if (refreshed > 0) {
            LumenLogger.info("Hot reloaded " + refreshed + " open inventor" + (refreshed == 1 ? "y" : "ies") + " for active viewers.");
        }
    }

    /**
     * Clears all tracked viewers. Called on full plugin disable.
     */
    public static void clear() {
        VIEWERS.clear();
    }

    private static void ensureListenerRegistered() {
        if (LISTENER_REGISTERED.compareAndSet(false, true)) {
            Bukkit.getPluginManager().registerEvents(new CloseListener(), Lumen.instance());
        }
    }

    private static void replaceContents(@NotNull Inventory current, @NotNull Inventory replacement) {
        current.clear();
        for (int i = 0; i < replacement.getSize(); i++) {
            current.setItem(i, replacement.getItem(i));
        }
    }

    private static void replaceTitle(@NotNull InventoryView view, @NotNull LumenInventoryHolder newHolder) {
        String newTitle = newHolder.title();
        if (newTitle == null) return;
        try {
            view.setTitle(newTitle);
        } catch (Throwable e) {
            LumenLogger.warning("[InventoryHotReload] Failed to update inventory title: " + e.getMessage());
        }
    }

    private static final class CloseListener implements Listener {

        @EventHandler
        public void onClose(@NotNull InventoryCloseEvent event) {
            VIEWERS.remove(event.getPlayer().getUniqueId());
        }
    }
}
