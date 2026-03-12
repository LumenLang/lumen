package dev.lumenlang.lumen.pipeline.java;

import dev.lumenlang.lumen.api.codegen.JavaOutput;
import dev.lumenlang.lumen.pipeline.java.compiled.ScriptRuntime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Accumulates lines of Java source code during script compilation.
 *
 * <p>Handlers call {@link #line(String)} to append individual Java statements or structural
 * tokens (method signatures, braces, etc.). The accumulated lines are later assembled into a
 * complete compilable class by
 * {@link ScriptRuntime}.
 *
 * <p>No indentation is added by this class; indentation within the generated class body is
 * applied uniformly by {@code ScriptRuntime.buildClass}.
 *
 * <p>Script source line markers can be attached via {@link #markScriptLine(int, String)} so
 * that runtime errors can be mapped back to the original {@code .luma} source.
 *
 * @see ScriptRuntime
 */
@SuppressWarnings("unused")
public class JavaBuilder implements JavaOutput {
    private final List<String> out = new ArrayList<>();
    private final Map<Integer, ScriptLineInfo> lineMap = new HashMap<>();

    /**
     * Appends a single Java source line.
     *
     * @param s the line to append (should not include a trailing newline)
     */
    @Override
    public void line(@NotNull String s) {
        out.add(s);
    }

    /**
     * Appends multiple Java source lines.
     *
     * @param s the collection of lines to append
     */
    public void lines(@NotNull Collection<String> s) {
        out.addAll(s);
    }

    /**
     * Associates the next emitted Java line with a script source location.
     *
     * <p>This should be called immediately before the Java line that corresponds
     * to the given script line, so that runtime errors can display the original
     * script source.
     *
     * @param scriptLine the 1-based line number in the script
     * @param source     the raw source text of the script line
     */
    public void markScriptLine(int scriptLine, @NotNull String source) {
        lineMap.put(out.size(), new ScriptLineInfo(scriptLine, source));
    }

    /**
     * Inserts a line at the given index, shifting all subsequent lines and
     * updating the script-line marker map so markers remain accurate.
     *
     * @param index the 0-based index to insert at
     * @param code  the line to insert
     */
    @Override
    public void insertLine(int index, @NotNull String code) {
        out.add(index, code);

        Map<Integer, ScriptLineInfo> shifted = new HashMap<>();
        for (var entry : lineMap.entrySet()) {
            int key = entry.getKey();
            shifted.put(key >= index ? key + 1 : key, entry.getValue());
        }
        lineMap.clear();
        lineMap.putAll(shifted);
    }

    /**
     * Returns the script-line info associated with the given Java line index, or null.
     *
     * @param javaLineIndex the 0-based index in the accumulated line list
     * @return the script line info, or null if no mapping exists
     */
    public @Nullable ScriptLineInfo scriptLineAt(int javaLineIndex) {
        return lineMap.get(javaLineIndex);
    }

    /**
     * Returns the accumulated list of Java source lines.
     *
     * @return the mutable line list
     */
    public @NotNull List<String> lines() {
        return out;
    }

    /**
     * Returns the number of lines accumulated so far.
     *
     * @return the current line count
     */
    @Override
    public int lineNum() {
        return out.size();
    }

    /**
     * Returns the full map of Java-line-index to script-line-info.
     *
     * @return the line map (unmodifiable view is not necessary since callers
     * are internal)
     */
    public @NotNull Map<Integer, ScriptLineInfo> lineMap() {
        return lineMap;
    }

    /**
     * Script source location info for a generated Java line.
     *
     * @param line   the 1-based script line number
     * @param source the raw source text
     */
    public record ScriptLineInfo(int line, @NotNull String source) {
    }
}
