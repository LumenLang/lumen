package dev.lumenlang.lumen.debug.hook;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Runtime hook interface for script instrumentation.
 *
 * <p>Implementations receive callbacks at each instrumented script line and at
 * each condition evaluation. The default no-op in {@link ScriptHooks} does nothing
 * when no debugger is attached.
 */
public interface ScriptHook {

    /**
     * Called at each instrumented script line with the current variable state.
     *
     * @param script the script name
     * @param line   the 1-based script line number
     * @param vars   a snapshot of all visible variables at this point, boxed
     */
    void onLine(@NotNull String script, int line, @NotNull Map<String, Object> vars);
}
