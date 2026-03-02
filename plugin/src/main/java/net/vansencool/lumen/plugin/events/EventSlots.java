package net.vansencool.lumen.plugin.events;

import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the mapping of Bukkit event classes to their registered event handler entries.
 *
 * <p>When a script binds an event handler method to a Bukkit event, it is stored in a {@link Slot}
 * associated with that event class. When the event is fired, the Slot dispatches the event to
 * all registered handlers for that event type.
 */
public final class EventSlots {

    private static final Map<Class<?>, Slot> slots = new ConcurrentHashMap<>();

    /**
     * Binds an event handler to the given Bukkit event type.
     *
     * <p>If no {@link Slot} exists for the event class yet, one is created and registered
     * with Bukkit. The handler entry is then added to the slot.
     *
     * @param event  the Bukkit event class to listen for
     * @param target the script instance that owns the handler
     * @param handle a MethodHandle pointing to the handler method on the target
     * @param <E>    the event type
     */
    public static synchronized <E extends Event> void bind(
            @NotNull Class<E> event,
            @NotNull Object target,
            @NotNull MethodHandle handle
    ) {
        Slot slot = slots.get(event);
        if (slot == null) {
            slot = new Slot(event);
            slots.put(event, slot);
            slot.register();
        }

        slot.add(target, handle);
    }

    /**
     * Removes all event handler entries associated with the given target instance
     * across all event types.
     *
     * @param target the script instance whose handlers should be cleared
     */
    public static synchronized void clearAll(@NotNull Object target) {
        for (Slot slot : slots.values())
            slot.remove(target);
    }
}
