package dev.lumenlang.lumen.debug.session;

import dev.lumenlang.lumen.debug.hook.ScriptHooks;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Listener for session events such as breakpoint hits.
 */
public interface DebugListener {

    /**
     * Called when execution reaches a breakpoint.
     *
     * @param script         the script name
     * @param line           the 1-based script line number
     * @param vars           a snapshot of all visible variables at this point
     * @param conditionTrace all condition evaluations that ran on this thread since the last breakpoint, in order
     */
    void onBreakpointHit(@NotNull String script, int line, @NotNull Map<String, Object> vars, @NotNull List<ScriptHooks.ConditionRecord> conditionTrace);
}
