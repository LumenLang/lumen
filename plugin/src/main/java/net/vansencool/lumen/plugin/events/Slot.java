package net.vansencool.lumen.plugin.events;

import net.vansencool.lumen.pipeline.java.compiled.LumenRuntimeException;
import net.vansencool.lumen.pipeline.java.compiled.ScriptSourceMap;
import net.vansencool.lumen.pipeline.logger.LumenLogger;
import net.vansencool.lumen.plugin.Lumen;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;

/**
 * Holds all registered handler entries for a single Bukkit event type and dispatches
 * incoming event to each of them.
 *
 * <p>A Slot is created the first time a handler is bound for a given event class.
 * Once registered with Bukkit, it stays registered for the lifetime of the plugin and
 * forwards every matching event to the current set of {@link Entry} handlers.
 */
public final class Slot {

    private final Class<? extends Event> event;
    private volatile Entry[] entries = new Entry[0];

    /**
     * Creates a new Slot for the given event type.
     *
     * @param event the Bukkit event class this slot handles
     */
    public Slot(@NotNull Class<? extends Event> event) {
        this.event = event;
    }

    /**
     * Registers this slot as a Bukkit event listener so that {@link #dispatch(Event)}
     * is called whenever the event fires.
     */
    public void register() {
        Bukkit.getPluginManager().registerEvent(
                event,
                new Listener() {
                },
                EventPriority.NORMAL,
                (listener, ev) -> dispatch(ev),
                Lumen.instance()
        );
    }

    /**
     * Adds a new handler entry to this slot.
     *
     * @param target the script instance to invoke the handler on
     * @param handle the MethodHandle pointing to the handler method
     */
    public void add(@NotNull Object target, @NotNull MethodHandle handle) {
        Entry[] old = entries;
        Entry[] next = new Entry[old.length + 1];
        System.arraycopy(old, 0, next, 0, old.length);
        next[old.length] = new Entry(target, handle);
        entries = next;
    }

    /**
     * Removes all handler entries associated with the given target instance.
     *
     * @param target the script instance whose handlers should be removed
     */
    public void remove(@NotNull Object target) {
        Entry[] old = entries;
        int n = 0;
        for (Entry e : old) if (e.target() != target) n++;
        Entry[] next = new Entry[n];
        int i = 0;
        for (Entry e : old) if (e.target() != target) next[i++] = e;
        entries = next;
    }

    /**
     * Dispatches the given event to all registered handler entries, catching and logging
     * any exceptions thrown by individual handlers.
     *
     * @param ev the Bukkit event to dispatch
     */
    public void dispatch(@NotNull Event ev) {
        if (!event.isInstance(ev)) return;
        for (Entry e : entries) {
            try {
                e.handle().invokeExact(e.target(), ev);
            } catch (LumenRuntimeException lre) {
                logLumenException(lre, e.target());
            } catch (Throwable t) {
                Throwable cause = t instanceof RuntimeException && t.getCause() != null ? t.getCause() : t;
                if (cause instanceof LumenRuntimeException lre) {
                    logLumenException(lre, e.target());
                } else {
                    String scriptClass = e.target().getClass().getSimpleName();
                    ScriptSourceMap.ScriptLineMapping mapping = ScriptSourceMap.findFromException(cause);
                    String locationInfo = mapping != null
                            ? " (script line " + mapping.scriptLine() + ": " + mapping.scriptSource()
                              + " | java line " + mapping.javaLine() + ")"
                            : "";
                    if (cause instanceof NullPointerException) {
                        LumenLogger.severe("[Script " + scriptClass + "] NullPointerException in events handler"
                                + locationInfo
                                + ". A variable is likely null, use 'if <var> is set:' to check before using it.");
                    } else {
                        LumenLogger.severe("[Script " + scriptClass + "] Runtime error in events handler"
                                + locationInfo + ": " + cause.getMessage());
                    }
                }
            }
        }
    }

    private static void logLumenException(LumenRuntimeException lre, Object target) {
        if (lre.scriptLine() > 0) {
            LumenLogger.severe("[Script] " + lre.getMessage());
            return;
        }
        ScriptSourceMap.ScriptLineMapping mapping = ScriptSourceMap.findFromException(lre);
        if (mapping != null) {
            LumenLogger.severe("[Script " + target.getClass().getSimpleName() + "] " + lre.getMessage()
                    + " (script line " + mapping.scriptLine() + ": " + mapping.scriptSource() + ")");
        } else {
            LumenLogger.severe("[Script] " + lre.getMessage());
        }
    }
}
