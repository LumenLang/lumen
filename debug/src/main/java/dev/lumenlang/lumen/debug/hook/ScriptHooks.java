package dev.lumenlang.lumen.debug.hook;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Static accessor for the active {@link ScriptHook}.
 *
 * <p>When no hook is installed, all methods delegate to a no-op implementation
 * to ensure zero runtime overhead.
 *
 * <p>This class lives in the debug module and is added to the compiler classpath
 * so that instrumented scripts can resolve it at runtime.
 */
public final class ScriptHooks {

    private static final ThreadLocal<List<ConditionRecord>> CONDITION_TRACE = ThreadLocal.withInitial(ArrayList::new);
    private static volatile ScriptHook ACTIVE = NoOpHook.INSTANCE;
    private static volatile Map<String, Object> SNIPPET_VARS = Map.of();
    private static volatile Map<String, SnippetVarMeta> SNIPPET_VAR_META = Map.of();
    private static volatile PrintStream SAVED_OUT;
    private static volatile PrintStream SAVED_ERR;
    private static volatile ByteArrayOutputStream CAPTURE_OUT;
    private static volatile ByteArrayOutputStream CAPTURE_ERR;

    private ScriptHooks() {
    }

    /**
     * Installs a script hook. Pass {@code null} to reset to the no-op default.
     *
     * @param hook the hook to install, or null
     */
    public static void install(@Nullable ScriptHook hook) {
        ACTIVE = hook != null ? hook : NoOpHook.INSTANCE;
    }

    /**
     * Returns the currently active hook.
     *
     * @return the active hook, never null
     */
    public static @NotNull ScriptHook active() {
        return ACTIVE;
    }

    /**
     * Builds a variable snapshot map from alternating name/value pairs.
     *
     * @param pairs alternating String name and Object value entries
     * @return an ordered map of variable names to their values
     */
    public static @NotNull Map<String, Object> vars(@NotNull Object... pairs) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) map.put((String) pairs[i], pairs[i + 1]);
        return map;
    }

    /**
     * Convenience delegate called by instrumented code at each script line.
     *
     * @param script the script name
     * @param line   the 1-based script line number
     * @param vars   the current variable snapshot
     */
    public static void onLine(@NotNull String script, int line, @NotNull Map<String, Object> vars) {
        ACTIVE.onLine(script, line, vars);
    }

    /**
     * Called by instrumented code at each condition evaluation. Records the result in the
     * thread-local trace and returns the result unmodified so it can be used inline inside
     * the condition expression.
     *
     * @param condId the condition identifier (script:line:cond or script:line:else)
     * @param source the original Lumen source text of the condition
     * @param line   the 1-based script line number
     * @param result the boolean result of the condition
     * @return {@code result}, unchanged
     */
    public static boolean onCondition(@NotNull String condId, @NotNull String source, int line, boolean result) {
        CONDITION_TRACE.get().add(new ConditionRecord(condId, source, line, result));
        return result;
    }

    /**
     * Clears the condition trace for the current thread. Call this at the start of a unit
     * of execution (e.g. before a snippet runs) to avoid accumulating stale entries.
     */
    public static void clearTrace() {
        CONDITION_TRACE.get().clear();
    }

    /**
     * Returns and clears the condition trace for the current thread.
     *
     * @return the accumulated condition records since the last clear, in evaluation order
     */
    public static @NotNull List<ConditionRecord> drainTrace() {
        List<ConditionRecord> snapshot = new ArrayList<>(CONDITION_TRACE.get());
        CONDITION_TRACE.get().clear();
        return snapshot;
    }

    /**
     * Sets the live variable references for snippet execution.
     *
     * @param vars the variables captured from a breakpoint, or empty map
     */
    public static void snippetVars(@NotNull Map<String, Object> vars) {
        SNIPPET_VARS = vars;
    }

    /**
     * Returns the live variable references for the current snippet.
     *
     * @return the snippet variables, never null
     */
    public static @NotNull Map<String, Object> snippetVars() {
        return SNIPPET_VARS;
    }

    /**
     * Sets the type metadata for snippet variables, used by the
     * {@code snippet var} expression at compile time.
     *
     * @param meta the variable type metadata, or empty map
     */
    public static void snippetVarMeta(@NotNull Map<String, SnippetVarMeta> meta) {
        SNIPPET_VAR_META = meta;
    }

    /**
     * Returns the type metadata for snippet variables.
     *
     * @return the metadata map, never null
     */
    public static @NotNull Map<String, SnippetVarMeta> snippetVarMeta() {
        return SNIPPET_VAR_META;
    }

    /**
     * Starts capturing {@link System#out} and {@link System#err}. Must be paired
     * with {@link #captureEnd()}. Safe to call from any thread.
     */
    public static void captureStart() {
        CAPTURE_OUT = new ByteArrayOutputStream();
        CAPTURE_ERR = new ByteArrayOutputStream();
        SAVED_OUT = System.out;
        SAVED_ERR = System.err;
        System.setOut(new PrintStream(CAPTURE_OUT));
        System.setErr(new PrintStream(CAPTURE_ERR));
    }

    /**
     * Stops capturing output and returns the captured stdout. Must be paired
     * with {@link #captureStart()}.
     *
     * @return all captured standard output text
     */
    public static @NotNull String captureEnd() {
        PrintStream out = SAVED_OUT;
        PrintStream err = SAVED_ERR;
        if (out != null) System.setOut(out);
        if (err != null) System.setErr(err);
        ByteArrayOutputStream capturedOut = CAPTURE_OUT;
        ByteArrayOutputStream capturedErr = CAPTURE_ERR;
        SAVED_OUT = null;
        SAVED_ERR = null;
        CAPTURE_OUT = null;
        CAPTURE_ERR = null;
        return capturedOut != null ? capturedOut.toString() : "";
    }

    /**
     * Returns the captured stderr without stopping capture.
     *
     * @return all captured standard error text
     */
    public static @NotNull String capturedStderr() {
        ByteArrayOutputStream capturedErr = CAPTURE_ERR;
        return capturedErr != null ? capturedErr.toString() : "";
    }

    private enum NoOpHook implements ScriptHook {
        INSTANCE;

        @Override
        public void onLine(@NotNull String script, int line, @NotNull Map<String, Object> vars) {
        }
    }

    /**
     * Type metadata for a snippet variable, used to generate correct casts
     * and register proper Lumen types at compile time.
     *
     * @param castType    the Java simple class name to cast to (e.g. "Player", "Integer")
     * @param refTypeId   the Lumen ref type ID (e.g. "PLAYER"), or null for primitives/strings
     * @param javaType    the Lumen java type (e.g. "int", "String"), or null for ref types
     * @param importClass the fully qualified class name to import, or null if standard
     */
    public record SnippetVarMeta(@NotNull String castType, @Nullable String refTypeId, @Nullable String javaType,
                                 @Nullable String importClass) {
    }

    /**
     * A single condition evaluation recorded during instrumented script execution.
     *
     * @param condId the condition identifier (script:line:cond or script:line:else)
     * @param source the Lumen source text of the condition
     * @param line   the 1-based script line number
     * @param result whether the condition evaluated to true (i.e. whether that branch was taken)
     */
    public record ConditionRecord(@NotNull String condId, @NotNull String source, int line, boolean result) {
    }
}
