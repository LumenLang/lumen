package net.vansencool.lumen.plugin.util;

import net.vansencool.lumen.plugin.text.LumenText;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global runtime registry for named Lumen inventory builders.
 *
 * <p>When a script declares a {@code register inventory "name":} block, the generated
 * method is discovered at bind time and registered here as a callable. Other scripts can
 * then open that inventory by name using {@code open inventory named "name" for player},
 * which invokes the builder method with the target player.
 *
 * <p>This enables cross-script inventory access. The inventory is built fresh each
 * time it is opened, so per-player customization (placeholders, permissions) works
 * naturally.
 */
@SuppressWarnings("unused")
public final class InventoryRegistry {

    private static final ConcurrentHashMap<String, Entry> REGISTRY = new ConcurrentHashMap<>();

    private InventoryRegistry() {
    }

    /**
     * Registers an inventory builder method by name.
     *
     * @param name     the programmatic inventory name
     * @param instance the script instance that owns the method
     * @param method   a MethodHandle to the builder method (signature: void(Player))
     */
    public static void register(@NotNull String name, @NotNull Object instance, @NotNull MethodHandle method) {
        REGISTRY.put(name, new Entry(instance, method));
    }

    /**
     * Opens a named inventory for the given player by invoking its registered builder method.
     *
     * @param name   the programmatic inventory name
     * @param player the player to build and open the inventory for
     */
    public static void open(@NotNull String name, @NotNull Player player) {
        Entry entry = REGISTRY.get(name);
        if (entry == null) {
            LumenText.send(player, "<red>Inventory '" + name + "' is not registered.");
            return;
        }
        try {
            entry.method.invoke(entry.instance, player);
        } catch (Throwable t) {
            LumenText.send(player, "<red>Failed to open inventory '" + name + "'.");
            throw new RuntimeException("Error invoking inventory builder for '" + name + "'", t);
        }
    }

    /**
     * Checks whether a named inventory builder is registered.
     *
     * @param name the programmatic inventory name
     * @return {@code true} if a builder is registered with that name
     */
    public static boolean isRegistered(@NotNull String name) {
        return REGISTRY.containsKey(name);
    }

    /**
     * Removes all registrations associated with a specific script instance.
     * Called when a script is reloaded or unloaded.
     *
     * @param instance the script instance to remove entries for
     */
    public static void clearInstance(@NotNull Object instance) {
        REGISTRY.values().removeIf(entry -> entry.instance == instance);
    }

    /**
     * Clears all registered inventory builders. Called on full plugin reload/disable.
     */
    public static void clear() {
        REGISTRY.clear();
    }

    private record Entry(@NotNull Object instance, @NotNull MethodHandle method) {
    }
}
