package dev.lumenlang.lumen.debug.transform;

import dev.lumenlang.lumen.api.emit.transform.CodeTransformer;
import dev.lumenlang.lumen.api.emit.transform.TaggedLine;
import dev.lumenlang.lumen.api.emit.transform.TransformContext;
import dev.lumenlang.lumen.debug.hook.ScriptHooks;
import net.vansencool.vanta.lexer.Lexer;
import net.vansencool.vanta.lexer.token.Token;
import net.vansencool.vanta.parser.Parser;
import net.vansencool.vanta.parser.ast.AstNode;
import net.vansencool.vanta.parser.ast.declaration.ClassDeclaration;
import net.vansencool.vanta.parser.ast.declaration.CompilationUnit;
import net.vansencool.vanta.parser.ast.declaration.MethodDeclaration;
import net.vansencool.vanta.parser.ast.expression.InstanceofExpression;
import net.vansencool.vanta.parser.ast.expression.NameExpression;
import net.vansencool.vanta.parser.ast.statement.BlockStatement;
import net.vansencool.vanta.parser.ast.statement.IfStatement;
import net.vansencool.vanta.parser.ast.statement.Statement;
import net.vansencool.vanta.parser.ast.statement.VariableDeclarationStatement;
import net.vansencool.vanta.parser.ast.statement.VariableDeclarator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Instruments compiled scripts with line-level checkpoint calls and compile-time
 * expression overrides. Uses Vanta's lexer and parser to produce an AST that
 * pinpoints proper statement boundaries, avoiding the brittle regex matching that
 * breaks on multi-line constructor calls or other complex expressions.
 */
public final class LineInstrumentTransformer implements CodeTransformer {

    private final Set<String> enabledScripts = ConcurrentHashMap.newKeySet();
    private final Map<String, ExprMeta> discoveredExpressions = new ConcurrentHashMap<>();
    private final Map<String, String> overrides = new ConcurrentHashMap<>();
    private final Map<String, CondMeta> discoveredConditions = new ConcurrentHashMap<>();
    private final Map<String, String> condOverrides = new ConcurrentHashMap<>();

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

    public void enable(@NotNull String scriptName) {
        enabledScripts.add(scriptName);
    }

    public void disable(@NotNull String scriptName) {
        enabledScripts.remove(scriptName);
    }

    public boolean enabled(@NotNull String scriptName) {
        return enabledScripts.contains(scriptName);
    }

    public void override(@NotNull String exprId, @NotNull String value) {
        overrides.put(exprId, value);
    }

    public void removeOverride(@NotNull String exprId) {
        overrides.remove(exprId);
    }

    public void removeAllOverrides(@NotNull String scriptName) {
        String prefix = scriptName + ":";
        overrides.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public @NotNull Map<String, String> overrides(@NotNull String scriptName) {
        Map<String, String> result = new HashMap<>();
        String prefix = scriptName + ":";
        for (var entry : overrides.entrySet()) {
            if (entry.getKey().startsWith(prefix)) result.put(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(result);
    }

    public @NotNull Map<String, ExprMeta> expressions(@NotNull String scriptName) {
        Map<String, ExprMeta> result = new HashMap<>();
        String prefix = scriptName + ":";
        for (var entry : discoveredExpressions.entrySet()) {
            if (entry.getKey().startsWith(prefix)) result.put(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(result);
    }

    public void overrideCondition(@NotNull String condId, @NotNull String mode) {
        condOverrides.put(condId, mode);
    }

    public void removeConditionOverride(@NotNull String condId) {
        condOverrides.remove(condId);
    }

    public void removeAllConditionOverrides(@NotNull String scriptName) {
        String prefix = scriptName + ":";
        condOverrides.keySet().removeIf(k -> k.startsWith(prefix));
    }

    public @NotNull Map<String, CondMeta> conditions(@NotNull String scriptName) {
        Map<String, CondMeta> result = new HashMap<>();
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

        StringBuilder sourceBuilder = new StringBuilder();
        for (TaggedLine line : lines) {
            sourceBuilder.append(line.code()).append("\n");
        }
        String source = sourceBuilder.toString();

        CompilationUnit cu;
        try {
            List<Token> tokens = new Lexer(source).tokenize();
            cu = new Parser(tokens).parse();
        } catch (Exception e) {
            return;
        }

        for (AstNode typeDecl : cu.typeDeclarations()) {
            if (!(typeDecl instanceof ClassDeclaration cd)) continue;
            for (AstNode member : cd.members()) {
                if (!(member instanceof MethodDeclaration md) || md.body() == null) continue;
                instrumentBlock(ctx, scriptName, lines, md.body(), new ArrayList<>());
            }
        }
    }

    /**
     * Walks a {@link BlockStatement} and instruments each top-level statement.
     *
     * <p>For variable declarations, captures the declarator metadata, applies any expression
     * override, and emits an {@code onLine} call after the full declaration. For if statements,
     * delegates to {@link #handleIf}. For everything else, inserts an {@code onLine} call before
     * the statement so the variable snapshot reflects the state at the start of that line.
     */
    private void instrumentBlock(@NotNull TransformContext ctx, @NotNull String scriptName, @NotNull List<TaggedLine> lines, @NotNull BlockStatement block, @NotNull List<VarInfo> outerVars) {
        List<VarInfo> declaredVars = new ArrayList<>(outerVars);
        int lastScriptLine = -1;

        for (Statement stmt : block.statements()) {
            int idx = stmt.line() - 1;
            if (idx < 0 || idx >= lines.size()) continue;
            TaggedLine line = lines.get(idx);
            int scriptLine = line.scriptLine();
            String scriptSrc = line.scriptSource();

            if (stmt instanceof VariableDeclarationStatement vds) {
                String varType = vds.type().toString();
                for (VariableDeclarator decl : vds.declarators()) {
                    String varName = decl.name();
                    if (varName.startsWith("$") || varName.startsWith("__")) continue;
                    declaredVars.add(new VarInfo(varName, varType));
                    if (scriptLine >= 1 && decl.initializer() != null) {
                        String exprId = scriptName + ":" + scriptLine + ":" + varName;
                        String src = scriptSrc != null ? scriptSrc : varName;
                        discoveredExpressions.put(exprId, new ExprMeta(exprId, src, varType, scriptLine));
                        String overrideValue = overrides.get(exprId);
                        if (overrideValue != null) {
                            ctx.replace(idx, leadingSpaces(line.code()) + buildOverrideLine(varType, varName, overrideValue));
                        }
                    }
                }
                if (scriptLine >= 1 && scriptLine != lastScriptLine) {
                    lastScriptLine = scriptLine;
                    ctx.insertAfter(endLineIndex(lines, stmt), leadingSpaces(line.code()) + "ScriptHooks.onLine(\"" + escapeJava(scriptName) + "\", " + scriptLine + ", " + buildVarsCapture(declaredVars) + ");");
                }
            } else if (stmt instanceof IfStatement ifs) {
                handleIf(ctx, scriptName, lines, ifs, declaredVars);
                if (scriptLine >= 1) lastScriptLine = scriptLine;
            } else {
                if (scriptLine >= 1 && scriptLine != lastScriptLine) {
                    lastScriptLine = scriptLine;
                    ctx.insertBefore(idx, leadingSpaces(line.code()) + "ScriptHooks.onLine(\"" + escapeJava(scriptName) + "\", " + scriptLine + ", " + buildVarsCapture(declaredVars) + ");");
                }
                if (stmt instanceof BlockStatement bs) instrumentBlock(ctx, scriptName, lines, bs, declaredVars);
            }
        }
    }

    /**
     * Rewrites the condition of an if statement to invoke {@link ScriptHooks#onCondition}, applying
     * any condition override and hoisting any pattern variables (e.g. {@code instanceof Foo __x})
     * into local declarations above the if. Recurses into both branches.
     */
    private void handleIf(@NotNull TransformContext ctx, @NotNull String scriptName, @NotNull List<TaggedLine> lines, @NotNull IfStatement ifs, @NotNull List<VarInfo> vars) {
        int idx = ifs.line() - 1;
        if (idx >= 0 && idx < lines.size()) {
            TaggedLine line = lines.get(idx);
            int scriptLine = line.scriptLine();
            String scriptSrc = line.scriptSource();

            if (scriptLine >= 1) {
                String condId = scriptName + ":" + scriptLine + ":cond";
                String src = scriptSrc != null ? scriptSrc : "if";
                discoveredConditions.put(condId, new CondMeta(condId, src, scriptLine));
                String override = condOverrides.get(condId);
                String origCode = line.code();
                int parenStart = origCode.indexOf('(');
                int parenEnd = origCode.lastIndexOf(')');
                if (parenStart >= 0 && parenEnd > parenStart) {
                    String rawCond = origCode.substring(parenStart + 1, parenEnd);
                    List<String> hoisted = new ArrayList<>();
                    if (ifs.condition() instanceof InstanceofExpression io && io.patternVariable() != null && io.patternVariable().startsWith("__")) {
                        String exprText = io.expression() instanceof NameExpression ne ? ne.name() : io.expression().toString();
                        String type = io.type().toString();
                        hoisted.add(type + " " + io.patternVariable() + " = " + exprText + " instanceof " + type + " ? (" + type + ")(" + exprText + ") : null;");
                    }
                    String innerCond = "true".equals(override) ? "true" : "false".equals(override) ? "false" : rawCond;
                    String prefix = origCode.substring(0, parenStart);
                    String suffix = origCode.substring(parenEnd + 1);
                    ctx.replace(idx, prefix + "(ScriptHooks.onCondition(\"" + escapeJava(condId) + "\", \"" + escapeJava(src) + "\", " + scriptLine + ", (" + innerCond + ")))" + suffix);
                    if (!hoisted.isEmpty()) ctx.insertLinesBefore(idx, hoisted);
                }
            }
        }

        if (ifs.thenBranch() instanceof BlockStatement thenBlock)
            instrumentBlock(ctx, scriptName, lines, thenBlock, vars);
        if (ifs.elseBranch() instanceof BlockStatement elseBlock)
            instrumentBlock(ctx, scriptName, lines, elseBlock, vars);
        else if (ifs.elseBranch() instanceof IfStatement nested) handleIf(ctx, scriptName, lines, nested, vars);
    }

    /**
     * Finds the index of the line that contains the closing semicolon of a statement that may span
     * multiple lines (e.g. a constructor call broken across several lines). Tracks paren, brace,
     * and bracket depth so the {@code ;} inside a nested expression doesn't terminate early.
     */
    private int endLineIndex(@NotNull List<TaggedLine> lines, @NotNull Statement stmt) {
        int start = stmt.line() - 1;
        if (start < 0 || start >= lines.size()) return -1;
        int depth = 0;
        boolean started = false;
        for (int i = start; i < lines.size(); i++) {
            String code = lines.get(i).code();
            for (int j = 0; j < code.length(); j++) {
                char c = code.charAt(j);
                if (c == '(' || c == '{' || c == '[') {
                    depth++;
                    started = true;
                } else if (c == ')' || c == '}' || c == ']') {
                    depth--;
                } else if (c == ';' && depth == 0) {
                    return i;
                }
            }
            if (started && depth == 0 && code.trim().endsWith(";")) return i;
        }
        return start;
    }

    private @NotNull String leadingSpaces(@NotNull String line) {
        int i = 0;
        while (i < line.length() && line.charAt(i) == ' ') i++;
        return line.substring(0, i);
    }

    public record ExprMeta(@NotNull String id, @NotNull String expression, @NotNull String type, int line) {
    }

    public record CondMeta(@NotNull String id, @NotNull String source, int line) {
    }

    private record VarInfo(@NotNull String name, @NotNull String type) {
    }
}
