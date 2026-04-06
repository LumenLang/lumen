package dev.lumenlang.lumen.plugin.events;

import dev.lumenlang.lumen.pipeline.java.compiled.LumenNullException;
import dev.lumenlang.lumen.pipeline.java.compiled.LumenRuntimeException;
import dev.lumenlang.lumen.pipeline.java.compiled.ScriptSourceMap;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.plugin.Lumen;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private final EventPriority priority;
    private final boolean ignoreCancelled;
    private volatile Entry[] entries = new Entry[0];

    /**
     * Creates a new Slot for the given event type, priority, and ignore-cancelled setting.
     *
     * @param event           the Bukkit event class this slot handles
     * @param priority        the priority at which this slot is registered with Bukkit
     * @param ignoreCancelled whether to skip firing when the event has already been cancelled
     */
    public Slot(@NotNull Class<? extends Event> event, @NotNull EventPriority priority, boolean ignoreCancelled) {
        this.event = event;
        this.priority = priority;
        this.ignoreCancelled = ignoreCancelled;
    }

    private static void logLumenException(LumenRuntimeException lre, Object target) {
        String scriptClass = target.getClass().getSimpleName();
        ScriptSourceMap.ScriptLineMapping mapping = lre.scriptLine() > 0
                ? null
                : ScriptSourceMap.findFromException(lre);
        LumenLogger.severe("[Script " + scriptClass + "] " + lre.getMessage());
        logSourceMapping(mapping);
    }

    private static void logSourceMapping(@Nullable ScriptSourceMap.ScriptLineMapping mapping) {
        if (mapping == null) return;
        LumenLogger.severe("  Script line " + mapping.scriptLine() + ": " + mapping.scriptSource());
        LumenLogger.severe("  Java line: " + mapping.javaLine());
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
                priority,
                (listener, ev) -> dispatch(ev),
                Lumen.instance(),
                ignoreCancelled
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
        for (Entry e : old) {
            if (e.target() != target) {
                next[i++] = e;
            } else {
                e.clear();
            }
        }
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
            Object target = e.target();
            MethodHandle handle = e.handle();
            if (target == null || handle == null) continue;
            try {
                handle.invokeExact(target, ev);
            } catch (LumenRuntimeException lre) {
                logLumenException(lre, target);
            } catch (Throwable t) {
                Throwable cause = t instanceof RuntimeException && t.getCause() != null ? t.getCause() : t;
                if (cause instanceof LumenRuntimeException lre) {
                    logLumenException(lre, target);
                } else {
                    String scriptClass = target.getClass().getSimpleName();
                    ScriptSourceMap.ScriptLineMapping mapping = ScriptSourceMap.findFromException(cause);
                    if (cause instanceof LumenNullException lne) {
                        LumenLogger.severe("[Script " + scriptClass + "] NullPointerException in events handler");
                        logSourceMapping(mapping);
                        LumenLogger.severe("  -> '" + lne.scriptVarName() + "' was null. Use 'if " + lne.scriptVarName() + " is set:' to guard it.");
                    } else if (cause instanceof NullPointerException npe) {
                        LumenLogger.severe("[Script " + scriptClass + "] NullPointerException in events handler");
                        logSourceMapping(mapping);
                        LumenLogger.severe("  -> " + ScriptSourceMap.formatNpeHint(npe));
                    } else {
                        String message = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
                        LumenLogger.severe("[Script " + scriptClass + "] Runtime error in events handler: " + message);
                        logSourceMapping(mapping);
                    }
                }
            }
        }
    }
}
