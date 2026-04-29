package dev.lumenlang.lumen.debug.session;

import dev.lumenlang.lumen.debug.hook.ScriptHook;
import dev.lumenlang.lumen.debug.hook.ScriptHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages breakpoints and variable snapshots for an active debug session.
 *
 * <p>Implements {@link ScriptHook} to receive line callbacks from instrumented
 * scripts. When a breakpoint is hit, the session captures the variable state
 * and notifies any registered {@link DebugListener}.
 */
public final class DebugSession implements ScriptHook {

    private final Map<String, Set<Integer>> breakpoints = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<BreakpointEvent> pendingEvents = new ConcurrentLinkedQueue<>();
    private volatile Map<String, Object> lastBreakpointVars = Map.of();
    private volatile DebugListener listener;

    /**
     * Sets the listener that receives session events.
     *
     * @param listener the listener, or null to clear
     */
    public void listener(@Nullable DebugListener listener) {
        this.listener = listener;
    }

    /**
     * Sets breakpoints for a script, replacing any previous breakpoints.
     *
     * @param script the script name
     * @param lines  the set of 1-based line numbers
     */
    public void breakpoints(@NotNull String script, @NotNull Set<Integer> lines) {
        if (lines.isEmpty()) {
            breakpoints.remove(script);
        } else {
            breakpoints.put(script, ConcurrentHashMap.newKeySet());
            breakpoints.get(script).addAll(lines);
        }
    }

    /**
     * Returns the current breakpoints for a script.
     *
     * @param script the script name
     * @return the breakpoint line numbers, may be empty
     */
    public @NotNull Set<Integer> breakpoints(@NotNull String script) {
        Set<Integer> lines = breakpoints.get(script);
        return lines != null ? Collections.unmodifiableSet(lines) : Set.of();
    }

    /**
     * Returns the live variable references from the last breakpoint hit.
     * These are the actual runtime objects, not string representations.
     *
     * @return the last breakpoint variables, may be empty if no breakpoint was hit
     */
    public @NotNull Map<String, Object> lastBreakpointVars() {
        return lastBreakpointVars;
    }

    @Override
    public void onLine(@NotNull String script, int line, @NotNull Map<String, Object> vars) {
        Set<Integer> bp = breakpoints.get(script);
        if (bp == null || !bp.contains(line)) return;

        lastBreakpointVars = new LinkedHashMap<>(vars);
        List<ScriptHooks.ConditionRecord> trace = ScriptHooks.drainTrace();
        pendingEvents.add(new BreakpointEvent(script, line, new LinkedHashMap<>(vars), List.copyOf(trace)));

        DebugListener l = listener;
        if (l != null) {
            l.onBreakpointHit(script, line, vars, trace);
        }
    }

    /**
     * Drains all buffered breakpoint events, returning them in order.
     *
     * @return the list of events since the last drain, never null
     */
    public @NotNull List<BreakpointEvent> drainEvents() {
        List<BreakpointEvent> events = new ArrayList<>();
        BreakpointEvent event;
        while ((event = pendingEvents.poll()) != null) events.add(event);
        return events;
    }

    /**
     * A captured breakpoint hit with all associated data.
     *
     * @param script         the script name
     * @param line           the 1-based line number
     * @param vars           the variable snapshot
     * @param conditionTrace the condition trace at the time of the hit
     */
    public record BreakpointEvent(@NotNull String script, int line, @NotNull Map<String, Object> vars,
                                  @NotNull List<ScriptHooks.ConditionRecord> conditionTrace) {
    }
}
