package dev.lumenlang.lumen.debug.server.override;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-script source text, expression replacements, and condition overrides.
 *
 * <p>Source text is updated as the editor enables debug. Replacements and overrides
 * are scoped by a {@code script:line:...} prefix and applied on every recompile via
 * {@link #prepareSource(String)}.
 */
public final class ScriptOverrideStore {

    private final @NotNull Map<String, String> scriptSources = new ConcurrentHashMap<>();
    private final @NotNull Map<String, String> conditionOverrides = new ConcurrentHashMap<>();
    private final @NotNull Map<String, String> expressionReplacements = new ConcurrentHashMap<>();

    /**
     * Returns whether a condition override value is a force-override (true / false / skip)
     * rather than a textual replacement.
     *
     * @param value the override value
     */
    public static boolean isForceOverride(@NotNull String value) {
        return "true".equals(value) || "false".equals(value) || "skip".equals(value);
    }

    private static @Nullable String parseVarName(@NotNull String exprId) {
        int lastColon = exprId.lastIndexOf(':');
        if (lastColon < 0) return null;
        return exprId.substring(lastColon + 1);
    }

    private static @NotNull String replaceExpressionText(@NotNull String line, @NotNull String varName, @NotNull String newExpression) {
        String trimmed = line.stripLeading();
        String indent = line.substring(0, line.length() - trimmed.length());
        if (trimmed.startsWith("set " + varName + " to ")) return indent + "set " + varName + " to " + newExpression;
        return line;
    }

    private static int parseLineNum(@NotNull String id) {
        int lastColon = id.lastIndexOf(':');
        if (lastColon < 0) return -1;
        int secondLastColon = id.lastIndexOf(':', lastColon - 1);
        if (secondLastColon < 0) return -1;
        try {
            return Integer.parseInt(id.substring(secondLastColon + 1, lastColon));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static @NotNull String replaceConditionText(@NotNull String line, @NotNull String newCondition) {
        String trimmed = line.stripLeading();
        String indent = line.substring(0, line.length() - trimmed.length());
        if (trimmed.startsWith("else if ") && trimmed.endsWith(":")) return indent + "else if " + newCondition + ":";
        if (trimmed.startsWith("if ") && trimmed.endsWith(":")) return indent + "if " + newCondition + ":";
        return line;
    }

    /**
     * Stores the source text for a script.
     *
     * @param script the script name
     * @param source the latest source text
     */
    public void source(@NotNull String script, @NotNull String source) {
        scriptSources.put(script, source);
    }

    /**
     * Removes any stored source for the script and returns the previous value if any.
     *
     * @param script the script name
     */
    public @Nullable String forget(@NotNull String script) {
        return scriptSources.remove(script);
    }

    /**
     * Replaces an expression by its id.
     *
     * @param exprId     the expression id
     * @param expression the new expression source
     */
    public void putExpression(@NotNull String exprId, @NotNull String expression) {
        expressionReplacements.put(exprId, expression);
    }

    /**
     * Removes a single expression replacement by id.
     *
     * @param exprId the expression id
     */
    public void removeExpression(@NotNull String exprId) {
        expressionReplacements.remove(exprId);
    }

    /**
     * Removes all expression replacements that belong to the given script.
     *
     * @param script the script name
     */
    public void clearExpressionsFor(@NotNull String script) {
        String prefix = script + ":";
        expressionReplacements.keySet().removeIf(k -> k.startsWith(prefix));
    }

    /**
     * Stores a condition override by id.
     *
     * @param condId the condition id
     * @param value  override value or replacement source
     */
    public void putCondition(@NotNull String condId, @NotNull String value) {
        conditionOverrides.put(condId, value);
    }

    /**
     * Removes a condition override by id.
     *
     * @param condId the condition id
     */
    public void removeCondition(@NotNull String condId) {
        conditionOverrides.remove(condId);
    }

    /**
     * Removes all condition overrides that belong to the given script.
     *
     * @param script the script name
     */
    public void clearConditionsFor(@NotNull String script) {
        String prefix = script + ":";
        conditionOverrides.keySet().removeIf(k -> k.startsWith(prefix));
    }

    /**
     * Returns the current expression replacement map.
     */
    public @NotNull Map<String, String> expressionReplacements() {
        return expressionReplacements;
    }

    /**
     * Returns the current condition override map.
     */
    public @NotNull Map<String, String> conditionOverrides() {
        return conditionOverrides;
    }

    /**
     * Returns the script source with all replacements applied, or {@code null} if unknown.
     *
     * @param script the script name
     */
    public @Nullable String prepareSource(@NotNull String script) {
        String source = scriptSources.get(script);
        if (source == null) return null;
        source = applyExpressions(source, script);
        return applyConditions(source, script);
    }

    private @NotNull String applyExpressions(@NotNull String source, @NotNull String script) {
        String prefix = script + ":";
        boolean has = expressionReplacements.keySet().stream().anyMatch(k -> k.startsWith(prefix));
        if (!has) return source;
        String[] lines = source.split("\n", -1);
        for (var entry : expressionReplacements.entrySet()) {
            if (!entry.getKey().startsWith(prefix)) continue;
            int lineNum = parseLineNum(entry.getKey());
            String varName = parseVarName(entry.getKey());
            if (lineNum < 1 || lineNum > lines.length || varName == null) continue;
            lines[lineNum - 1] = replaceExpressionText(lines[lineNum - 1], varName, entry.getValue());
        }
        return String.join("\n", lines);
    }

    private @NotNull String applyConditions(@NotNull String source, @NotNull String script) {
        String prefix = script + ":";
        boolean has = conditionOverrides.entrySet().stream().anyMatch(e -> e.getKey().startsWith(prefix) && !isForceOverride(e.getValue()));
        if (!has) return source;
        String[] lines = source.split("\n", -1);
        for (var entry : conditionOverrides.entrySet()) {
            if (!entry.getKey().startsWith(prefix) || isForceOverride(entry.getValue())) continue;
            int lineNum = parseLineNum(entry.getKey());
            if (lineNum < 1 || lineNum > lines.length) continue;
            lines[lineNum - 1] = replaceConditionText(lines[lineNum - 1], entry.getValue());
        }
        return String.join("\n", lines);
    }
}
