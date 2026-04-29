package dev.lumenlang.lumen.debug.transform;

import dev.lumenlang.lumen.api.emit.transform.CodeTransformer;
import dev.lumenlang.lumen.api.emit.transform.TaggedLine;
import dev.lumenlang.lumen.api.emit.transform.TransformContext;
import dev.lumenlang.lumen.debug.hook.ScriptHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Instruments compiled scripts with line-level checkpoint calls and
 * compile-time expression overrides.
 *
 * <p>For each Java line that maps to a script source line, this transformer inserts
 * a call to {@link ScriptHooks#onLine(String, int, Map)} after variable
 * declarations (capturing the new value) and before other executable lines.
 */
public final class LineInstrumentTransformer implements CodeTransformer {

    private static final Pattern METHOD_SIGNATURE = Pattern.compile("^(public|private|protected)\\s+.*\\{\\s*$");
    private static final Pattern VAR_DECL = Pattern.compile("^(?:final\\s+)?([\\w.<>\\[\\]]+)\\s+(\\w+)\\s*=\\s*(.+);$");
    private static final Pattern VAR_SIMPLE = Pattern.compile("^([\\w.<>\\[\\]]+)\\s+(\\w+)\\s*;$");
    private static final Pattern COND_IF = Pattern.compile("^if\\s*\\((.+)\\)\\s*\\{$");
    private static final Pattern COND_ELSE_IF = Pattern.compile("^else\\s+if\\s*\\((.+)\\)\\s*\\{$");
    private static final Pattern COND_ELSE = Pattern.compile("^else\\s*\\{$");
    private static final Pattern INSTANCEOF_PATTERN_VAR = Pattern.compile("([\\w.()]+)\\s+instanceof\\s+(\\w+(?:<[^>]*>)?)\\s+(__\\w+)");
    private final Set<String> enabledScripts = ConcurrentHashMap.newKeySet();
    private final Map<String, ExprMeta> discoveredExpressions = new ConcurrentHashMap<>();
    private final Map<String, String> overrides = new ConcurrentHashMap<>();
    private final Map<String, CondMeta> discoveredConditions = new ConcurrentHashMap<>();
    private final Map<String, String> condOverrides = new ConcurrentHashMap<>();

    private static @NotNull List<String> extractPatternVars(@NotNull String cond) {
        List<String> result = new ArrayList<>();
        Matcher m = INSTANCEOF_PATTERN_VAR.matcher(cond);
        while (m.find()) {
            String expr = m.group(1);
            String type = m.group(2);
            String var = m.group(3);
            result.add(type + " " + var + " = " + expr + " instanceof " + type + " ? (" + type + ")(" + expr + ") : null;");
        }
        return result;
    }

    private static @NotNull String replacePatternVars(@NotNull String cond) {
        return INSTANCEOF_PATTERN_VAR.matcher(cond).replaceAll(m -> m.group(3) + " != null");
    }

    private static boolean isChainedBlock(@NotNull String trimmed) {
        return trimmed.startsWith("else") || trimmed.startsWith("} else") || trimmed.startsWith("catch") || trimmed.startsWith("} catch") || trimmed.startsWith("finally") || trimmed.startsWith("} finally");
    }

    private static int countLeadingCloses(@NotNull String trimmed) {
        int count = 0;
        int i = 0;
        while (i < trimmed.length()) {
            char c = trimmed.charAt(i);
            if (c == '}') {
                count++;
                i++;
            } else if (c == ' ') i++;
            else break;
        }
        return count;
    }

    private static int netBraces(@NotNull String code) {
        int opens = 0, closes = 0;
        boolean inString = false;
        char stringChar = 0;
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++;
                    continue;
                }
                if (c == stringChar) inString = false;
            } else if (c == '"' || c == '\'') {
                inString = true;
                stringChar = c;
            } else if (c == '{') {
                opens++;
            } else if (c == '}') {
                closes++;
            }
        }
        return opens - closes;
    }

    private static int trailingOpens(@NotNull String trimmed, int leadingCloses) {
        return Math.max(0, netBraces(trimmed) + leadingCloses);
    }

    private static @NotNull String buildVarsCapture(@NotNull List<VarInfo> vars) {
        if (vars.isEmpty()) return "Map.of()";
        StringBuilder sb = new StringBuilder("ScriptHooks.vars(");
        for (int i = 0; i < vars.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(vars.get(i).name).append("\", ").append(boxExpression(vars.get(i).name, vars.get(i).type));
        }
        sb.append(")");
        return sb.toString();
    }

    private static @NotNull String buildOverrideLine(@NotNull String type, @NotNull String varName, @NotNull String overrideValue) {
        return type + " " + varName + " = " + buildLiteral(overrideValue, type) + ";";
    }

    private static @NotNull String buildLiteral(@NotNull String value, @NotNull String type) {
        return switch (type) {
            case "int", "boolean", "double" -> value;
            case "long" -> value + "L";
            case "float" -> value + "F";
            case "byte" -> "(byte) " + value;
            case "short" -> "(short) " + value;
            case "char" -> "'" + escapeJava(value) + "'";
            case "String" -> "\"" + escapeJava(value) + "\"";
            default -> inferLiteral(value);
        };
    }

    private static @NotNull String inferLiteral(@NotNull String value) {
        if ("true".equals(value) || "false".equals(value)) return value;
        if ("null".equals(value)) return "null";
        try {
            Integer.parseInt(value);
            return value;
        } catch (NumberFormatException ignored) {
        }
        try {
            Double.parseDouble(value);
            return value;
        } catch (NumberFormatException ignored) {
        }
        return "\"" + escapeJava(value) + "\"";
    }

    private static @NotNull String boxExpression(@NotNull String varName, @NotNull String type) {
        return switch (type) {
            case "int" -> "Integer.valueOf(" + varName + ")";
            case "long" -> "Long.valueOf(" + varName + ")";
            case "double" -> "Double.valueOf(" + varName + ")";
            case "float" -> "Float.valueOf(" + varName + ")";
            case "boolean" -> "Boolean.valueOf(" + varName + ")";
            case "byte" -> "Byte.valueOf(" + varName + ")";
            case "short" -> "Short.valueOf(" + varName + ")";
            case "char" -> "Character.valueOf(" + varName + ")";
            default -> varName;
        };
    }

    private static @NotNull String escapeJava(@NotNull String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Enables instrumentation for the given script.
     *
     * @param scriptName the script file name
     */
    public void enable(@NotNull String scriptName) {
        enabledScripts.add(scriptName);
    }

    /**
     * Disables instrumentation for the given script.
     *
     * @param scriptName the script file name
     */
    public void disable(@NotNull String scriptName) {
        enabledScripts.remove(scriptName);
    }

    /**
     * Returns whether instrumentation is enabled for the given script.
     *
     * @param scriptName the script file name
     * @return true if enabled
     */
    public boolean enabled(@NotNull String scriptName) {
        return enabledScripts.contains(scriptName);
    }

    /**
     * Sets a compile-time override for an expression. On the next recompilation,
     * the original assignment is replaced with the hardcoded value.
     *
     * @param exprId the expression identifier (script:line:varName)
     * @param value  the override value as a string
     */
    public void override(@NotNull String exprId, @NotNull String value) {
        overrides.put(exprId, value);
    }

    /**
     * Removes a compile-time override.
     *
     * @param exprId the expression identifier
     */
    public void removeOverride(@NotNull String exprId) {
        overrides.remove(exprId);
    }

    /**
     * Removes all overrides for the given script.
     *
     * @param scriptName the script file name
     */
    public void removeAllOverrides(@NotNull String scriptName) {
        String prefix = scriptName + ":";
        overrides.keySet().removeIf(k -> k.startsWith(prefix));
    }

    /**
     * Returns all active overrides for the given script.
     *
     * @param scriptName the script file name
     * @return unmodifiable map of expression ID to override value
     */
    public @NotNull Map<String, String> overrides(@NotNull String scriptName) {
        ConcurrentHashMap<String, String> result = new ConcurrentHashMap<>();
        String prefix = scriptName + ":";
        for (var entry : overrides.entrySet()) {
            if (entry.getKey().startsWith(prefix)) result.put(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns all discovered expressions for the given script.
     *
     * @param scriptName the script file name
     * @return unmodifiable map of expression ID to metadata
     */
    public @NotNull Map<String, ExprMeta> expressions(@NotNull String scriptName) {
        ConcurrentHashMap<String, ExprMeta> result = new ConcurrentHashMap<>();
        String prefix = scriptName + ":";
        for (var entry : discoveredExpressions.entrySet()) {
            if (entry.getKey().startsWith(prefix)) result.put(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Sets a compile-time condition override. Supported values are
     * {@code "true"}, {@code "false"}, and {@code "skip"} (for else blocks).
     *
     * @param condId the condition identifier (script:line:cond or script:line:else)
     * @param mode   the override mode
     */
    public void overrideCondition(@NotNull String condId, @NotNull String mode) {
        condOverrides.put(condId, mode);
    }

    /**
     * Removes a condition override.
     *
     * @param condId the condition identifier
     */
    public void removeConditionOverride(@NotNull String condId) {
        condOverrides.remove(condId);
    }

    /**
     * Removes all condition overrides for the given script.
     *
     * @param scriptName the script file name
     */
    public void removeAllConditionOverrides(@NotNull String scriptName) {
        String prefix = scriptName + ":";
        condOverrides.keySet().removeIf(k -> k.startsWith(prefix));
    }

    /**
     * Returns all active condition overrides for the given script.
     *
     * @param scriptName the script file name
     * @return unmodifiable map of condition ID to override mode
     */
    public @NotNull Map<String, String> conditionOverrides(@NotNull String scriptName) {
        ConcurrentHashMap<String, String> result = new ConcurrentHashMap<>();
        String prefix = scriptName + ":";
        for (var entry : condOverrides.entrySet()) {
            if (entry.getKey().startsWith(prefix)) result.put(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns all discovered conditions for the given script.
     *
     * @param scriptName the script file name
     * @return unmodifiable map of condition ID to metadata
     */
    public @NotNull Map<String, CondMeta> conditions(@NotNull String scriptName) {
        ConcurrentHashMap<String, CondMeta> result = new ConcurrentHashMap<>();
        String prefix = scriptName + ":";
        for (var entry : discoveredConditions.entrySet()) {
            if (entry.getKey().startsWith(prefix)) result.put(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public @Nullable List<String> tags() {
        return null;
    }

    @Override
    public void transform(@NotNull TransformContext ctx) {
        String scriptName = ctx.codegen().scriptName();
        if (!enabledScripts.contains(scriptName)) return;

        List<TaggedLine> lines = ctx.lines();
        if (lines.stream().anyMatch(l -> l.code().contains("ScriptHooks.onLine("))) return;

        discoveredExpressions.keySet().removeIf(k -> k.startsWith(scriptName + ":"));
        discoveredConditions.keySet().removeIf(k -> k.startsWith(scriptName + ":"));

        ctx.codegen().addImport(ScriptHooks.class.getName());
        ctx.codegen().addImport("java.util.Map");

        List<VarInfo> declaredVars = new ArrayList<>();
        int depth = 0;
        int lastScriptLine = -1;

        for (int i = 0; i < lines.size(); i++) {
            TaggedLine line = lines.get(i);
            String trimmed = line.code().trim();

            int lc = countLeadingCloses(trimmed);
            if (lc > 0) {
                depth = Math.max(0, depth - lc);
                final int maxDepth = depth;
                declaredVars.removeIf(v -> v.depth > maxDepth);
            }

            Matcher declMatch = VAR_DECL.matcher(trimmed);
            if (declMatch.matches()) {
                String varType = declMatch.group(1);
                String varName = declMatch.group(2);
                if (!varName.startsWith("$") && !varName.startsWith("__")) {
                    declaredVars.add(new VarInfo(varName, varType, depth));
                    if (line.scriptLine() >= 1 && !trimmed.startsWith("final")) {
                        String exprId = scriptName + ":" + line.scriptLine() + ":" + varName;
                        String src = line.scriptSource() != null ? line.scriptSource() : varName;
                        discoveredExpressions.put(exprId, new ExprMeta(exprId, src, varType, line.scriptLine()));
                        String overrideValue = overrides.get(exprId);
                        if (overrideValue != null) {
                            ctx.replace(i, buildOverrideLine(varType, varName, overrideValue));
                        }
                    }
                    if (line.scriptLine() >= 1 && line.scriptLine() != lastScriptLine && line.scriptSource() != null) {
                        lastScriptLine = line.scriptLine();
                        ctx.insertAfter(i, "ScriptHooks.onLine(\"" + escapeJava(scriptName) + "\", " + line.scriptLine() + ", " + buildVarsCapture(declaredVars) + ");");
                    }
                }
                depth += trailingOpens(trimmed, lc);
                continue;
            }

            Matcher simpleMatch = VAR_SIMPLE.matcher(trimmed);
            if (simpleMatch.matches()) {
                String varName = simpleMatch.group(2);
                if (!varName.startsWith("$") && !varName.startsWith("__")) {
                    declaredVars.add(new VarInfo(varName, simpleMatch.group(1), depth));
                }
                depth += trailingOpens(trimmed, lc);
                continue;
            }

            if (METHOD_SIGNATURE.matcher(trimmed).matches()) {
                depth = 0;
                declaredVars.clear();
                lastScriptLine = -1;
                depth += trailingOpens(trimmed, lc);
                continue;
            }

            if (trimmed.isEmpty() || trimmed.equals("{") || trimmed.equals("}") || trimmed.startsWith("//") || trimmed.startsWith("@") || isChainedBlock(trimmed)) {
                if (line.scriptLine() >= 1) {
                    Matcher elseIfMatch = COND_ELSE_IF.matcher(trimmed);
                    if (elseIfMatch.matches()) {
                        String condId = scriptName + ":" + line.scriptLine() + ":cond";
                        String src = line.scriptSource() != null ? line.scriptSource() : trimmed;
                        discoveredConditions.put(condId, new CondMeta(condId, src, line.scriptLine()));
                        String override = condOverrides.get(condId);
                        String innerCond = "true".equals(override) ? "true" : "false".equals(override) ? "false" : elseIfMatch.group(1);
                        ctx.replace(i, "else if (ScriptHooks.onCondition(\"" + escapeJava(condId) + "\", \"" + escapeJava(src) + "\", " + line.scriptLine() + ", (" + innerCond + "))) {");
                    } else if (COND_ELSE.matcher(trimmed).matches()) {
                        String condId = scriptName + ":" + line.scriptLine() + ":else";
                        String src = line.scriptSource() != null ? line.scriptSource() : "else";
                        discoveredConditions.put(condId, new CondMeta(condId, src, line.scriptLine()));
                        String override = condOverrides.get(condId);
                        if ("skip".equals(override)) ctx.replace(i, "else if (false) {");
                        else
                            ctx.insertAfter(i, "ScriptHooks.onCondition(\"" + escapeJava(condId) + "\", \"" + escapeJava(src) + "\", " + line.scriptLine() + ", true);");
                    }
                }
                depth += trailingOpens(trimmed, lc);
                continue;
            }

            if (line.scriptLine() >= 1) {
                Matcher ifMatch = COND_IF.matcher(trimmed);
                if (ifMatch.matches()) {
                    String condId = scriptName + ":" + line.scriptLine() + ":cond";
                    String src = line.scriptSource() != null ? line.scriptSource() : trimmed;
                    discoveredConditions.put(condId, new CondMeta(condId, src, line.scriptLine()));
                    String override = condOverrides.get(condId);
                    String rawCond = ifMatch.group(1);
                    List<String> hoisted = extractPatternVars(rawCond);
                    String adjustedCond = hoisted.isEmpty() ? rawCond : replacePatternVars(rawCond);
                    String innerCond = "true".equals(override) ? "true" : "false".equals(override) ? "false" : adjustedCond;
                    ctx.replace(i, "if (ScriptHooks.onCondition(\"" + escapeJava(condId) + "\", \"" + escapeJava(src) + "\", " + line.scriptLine() + ", (" + innerCond + "))) {");
                    if (!hoisted.isEmpty()) ctx.insertLinesBefore(i, hoisted);
                }
            }

            if (line.scriptLine() >= 1 && line.scriptLine() != lastScriptLine) {
                lastScriptLine = line.scriptLine();
                ctx.insertBefore(i, "ScriptHooks.onLine(\"" + escapeJava(scriptName) + "\", " + line.scriptLine() + ", " + buildVarsCapture(declaredVars) + ");");
            }

            depth += trailingOpens(trimmed, lc);
        }
    }

    /**
     * Metadata about a discovered expression in a compiled script.
     *
     * @param id         the stable expression identifier (script:line:varName)
     * @param expression the original script source text for this assignment
     * @param type       the Java type name
     * @param line       the 1-based script line number
     */
    public record ExprMeta(@NotNull String id, @NotNull String expression, @NotNull String type, int line) {
    }

    /**
     * Metadata about a discovered condition in a compiled script.
     *
     * @param id     the stable condition identifier (script:line:cond or script:line:else)
     * @param source the original Lumen source text for this condition
     * @param line   the 1-based script line number
     */
    public record CondMeta(@NotNull String id, @NotNull String source, int line) {
    }

    private record VarInfo(@NotNull String name, @NotNull String type, int depth) {
    }
}
