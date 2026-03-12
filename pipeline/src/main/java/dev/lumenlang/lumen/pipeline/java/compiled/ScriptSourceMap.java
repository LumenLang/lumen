package dev.lumenlang.lumen.pipeline.java.compiled;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runtime registry of generated Java source code for loaded scripts.
 *
 * <p>When a script is compiled and loaded, its generated Java source is stored here
 * keyed by fully-qualified class name. When a runtime exception occurs inside a
 * generated script class, this registry is consulted to map the Java line number
 * from the stack trace back to the nearest {@code @lumen:} source comment, giving
 * the user a meaningful script-level error location.
 *
 * <p>Sources are registered during script loading and removed on unload so the
 * map does not grow unbounded.
 */
@SuppressWarnings("unused")
public final class ScriptSourceMap {

    private static final String COMPILED_PREFIX = "dev.lumenlang.lumen.java.compiled.";
    private static final String MARKER_PREFIX = "// @lumen:";

    private static final Pattern NPE_INVOKE =
            Pattern.compile("Cannot invoke \"([^\"]+)\" because \"([^\"]+)\" is null");
    private static final Pattern NPE_READ_FIELD =
            Pattern.compile("Cannot read field \"([^\"]+)\" because \"([^\"]+)\" is null");

    private static final Map<String, String> SOURCES = new ConcurrentHashMap<>();

    private ScriptSourceMap() {
    }

    /**
     * Registers the generated Java source for a script class.
     *
     * @param fqcn       the fully-qualified class name
     * @param javaSource the complete generated Java source string
     */
    public static void register(@NotNull String fqcn, @NotNull String javaSource) {
        SOURCES.put(fqcn, javaSource);
    }

    /**
     * Removes the generated source for a script class.
     *
     * @param fqcn the fully-qualified class name
     */
    public static void unregister(@NotNull String fqcn) {
        SOURCES.remove(fqcn);
    }

    /**
     * Removes all generated sources whose FQCN matches the given script name pattern.
     *
     * @param normalizedClassName the normalized class name (without package prefix)
     */
    public static void unregisterByClassName(@NotNull String normalizedClassName) {
        String prefix = COMPILED_PREFIX + normalizedClassName;
        SOURCES.keySet().removeIf(k -> k.equals(prefix) || k.startsWith(prefix + "$"));
    }

    /**
     * Formats a human-readable hint from a {@link NullPointerException} thrown inside
     * generated script code. Parses Java 17 helpful NPE messages to extract the null
     * variable name and the method or field that was called on it.
     *
     * @param npe the null pointer exception to describe
     * @return a hint string suitable for a script error log message
     */
    public static @NotNull String formatNpeHint(@NotNull NullPointerException npe) {
        String msg = npe.getMessage();
        if (msg == null || msg.isBlank()) {
            return "A variable was null. Use 'if <var> is set:' to guard it before use.";
        }

        Matcher invoke = NPE_INVOKE.matcher(msg);
        if (invoke.find()) {
            String fqMethod = invoke.group(1);
            String varName = invoke.group(2);
            String simpleMethod = fqMethod.contains(".")
                    ? fqMethod.substring(fqMethod.lastIndexOf('.') + 1)
                    : fqMethod;
            return "'" + varName + "' was null (tried to call " + simpleMethod
                    + "). Use 'if " + varName + " is set:' to guard it.";
        }

        Matcher readField = NPE_READ_FIELD.matcher(msg);
        if (readField.find()) {
            String field = readField.group(1);
            String varName = readField.group(2);
            return "'" + varName + "' was null (tried to read field " + field
                    + "). Use 'if " + varName + " is set:' to guard it.";
        }

        return msg;
    }

    /**
     * Removes all stored sources.
     */
    public static void clear() {
        SOURCES.clear();
    }

    /**
     * Attempts to find the nearest script source line for a given Java line number
     * in the generated source of the specified class.
     *
     * <p>Scans backwards from the given Java line looking for the nearest
     * {@code // @lumen:N: source text} comment. Returns {@code null} if no mapping
     * is found or the class is not registered.
     *
     * @param fqcn     the fully-qualified class name
     * @param javaLine the 1-based Java line number from a stack trace element
     * @return the mapping, or null if no matching marker was found
     */
    public static @Nullable ScriptLineMapping findScriptLine(@NotNull String fqcn, int javaLine) {
        String source = SOURCES.get(fqcn);
        if (source == null) return null;

        String[] lines = source.split("\n", -1);
        if (javaLine < 1 || javaLine > lines.length) return null;

        for (int i = javaLine - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.startsWith(MARKER_PREFIX)) {
                return parseMarker(line, javaLine);
            }
        }
        return null;
    }

    /**
     * Attempts to resolve script-line info from an exception's stack trace.
     *
     * <p>Walks the stack trace elements looking for frames in a generated script class
     * (under {@code dev.lumenlang.lumen.java.compiled}), then uses the source map
     * to find the nearest {@code @lumen:} comment.
     *
     * @param throwable the exception to inspect
     * @return the mapping if found, or null
     */
    public static @Nullable ScriptLineMapping findFromException(@NotNull Throwable throwable) {
        for (StackTraceElement element : throwable.getStackTrace()) {
            String className = element.getClassName();
            if (className.startsWith(COMPILED_PREFIX) && element.getLineNumber() > 0) {
                ScriptLineMapping mapping = findScriptLine(className, element.getLineNumber());
                if (mapping != null) return mapping;
            }
        }
        return null;
    }

    private static @Nullable ScriptLineMapping parseMarker(@NotNull String trimmedLine, int javaLine) {
        int start = MARKER_PREFIX.length();
        int colonAfterNum = trimmedLine.indexOf(':', start);
        if (colonAfterNum <= start) return null;

        try {
            int scriptLine = Integer.parseInt(trimmedLine.substring(start, colonAfterNum));
            String scriptSource = colonAfterNum + 2 <= trimmedLine.length()
                    ? trimmedLine.substring(colonAfterNum + 2)
                    : "";
            return new ScriptLineMapping(scriptLine, scriptSource, javaLine);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * A resolved mapping from a Java line number back to a script source line.
     *
     * @param scriptLine   the 1-based line number in the original .luma script
     * @param scriptSource the raw source text of that script line
     * @param javaLine     the 1-based Java line number from the stack trace
     */
    public record ScriptLineMapping(int scriptLine, @NotNull String scriptSource, int javaLine) {
    }
}
