package dev.lumenlang.lumen.plugin.defaults.emit.transformer.global;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.emit.transform.CodeTransformer;
import dev.lumenlang.lumen.api.emit.transform.TaggedLine;
import dev.lumenlang.lumen.api.emit.transform.TransformContext;
import dev.lumenlang.lumen.plugin.defaults.emit.hook.GlobalVarLoadHook;
import net.vansencool.vanta.lexer.Lexer;
import net.vansencool.vanta.parser.Parser;
import net.vansencool.vanta.parser.ast.AstNode;
import net.vansencool.vanta.parser.ast.declaration.ClassDeclaration;
import net.vansencool.vanta.parser.ast.declaration.CompilationUnit;
import net.vansencool.vanta.parser.ast.declaration.MethodDeclaration;
import net.vansencool.vanta.parser.ast.expression.AssignmentExpression;
import net.vansencool.vanta.parser.ast.expression.CastExpression;
import net.vansencool.vanta.parser.ast.expression.Expression;
import net.vansencool.vanta.parser.ast.expression.LambdaExpression;
import net.vansencool.vanta.parser.ast.expression.MethodCallExpression;
import net.vansencool.vanta.parser.ast.expression.NameExpression;
import net.vansencool.vanta.parser.ast.expression.ParenExpression;
import net.vansencool.vanta.parser.ast.expression.UnaryExpression;
import net.vansencool.vanta.parser.ast.statement.BlockStatement;
import net.vansencool.vanta.parser.ast.statement.ExpressionStatement;
import net.vansencool.vanta.parser.ast.statement.ForStatement;
import net.vansencool.vanta.parser.ast.statement.IfStatement;
import net.vansencool.vanta.parser.ast.statement.ReturnStatement;
import net.vansencool.vanta.parser.ast.statement.Statement;
import net.vansencool.vanta.parser.ast.statement.WhileStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transformer that flushes mutated globals back to storage at the end of every method body.
 *
 * <p>Parses the emitted Java with Vanta to find each method body, identifies which
 * {@link GlobalVarLoadHook}-loaded globals are assigned within, and inserts
 * {@code Storage.set(key, name)} calls before each {@code return} and before the closing
 * brace of the method body.
 */
@Registration(order = -1990)
@SuppressWarnings("unused")
public final class GlobalVarWritebackTransformer implements CodeTransformer {
    public static final String TAG = "global-var-writeback";
    private static final String MARKER = ".set(\"";
    private static final Pattern LOAD_LINE = Pattern.compile("^\\s*(\\w+)\\s*=\\s*\\(?(?:\\w+(?:<[^>]+>)?)?\\)?\\s*(GlobalVars|PersistentVars)\\.get\\(\"([^\"]+)\".*$");

    @Call
    public void register(@NotNull LumenAPI api) {
        api.transformers().register(this);
    }

    @Override
    public @Nullable List<String> tags() {
        return List.of(TAG);
    }

    @Override
    public void transform(@NotNull TransformContext ctx) {
        for (TaggedLine line : ctx.lines()) {
            if (TAG.equals(line.tag())) return;
        }

        Map<String, GlobalInfo> globals = collectGlobals(ctx);
        if (globals.isEmpty()) return;

        CompilationUnit unit;
        try {
            unit = new Parser(new Lexer(ctx.fullSource()).tokenize()).parse();
        } catch (RuntimeException e) {
            return;
        }

        for (AstNode top : unit.typeDeclarations()) {
            if (top instanceof ClassDeclaration cls) processClass(cls, ctx, globals);
        }
    }

    private static @NotNull Map<String, GlobalInfo> collectGlobals(@NotNull TransformContext ctx) {
        Map<String, GlobalInfo> out = new LinkedHashMap<>();
        for (TaggedLine line : ctx.lines()) {
            if (!GlobalVarLoadHook.TAG.equals(line.tag())) continue;
            Matcher m = LOAD_LINE.matcher(line.code());
            if (!m.matches()) continue;
            out.putIfAbsent(m.group(1), new GlobalInfo(m.group(1), m.group(2), m.group(3)));
        }
        return out;
    }

    private static void processClass(@NotNull ClassDeclaration cls, @NotNull TransformContext ctx, @NotNull Map<String, GlobalInfo> globals) {
        for (AstNode member : cls.members()) {
            if (member instanceof MethodDeclaration md && md.body() != null) processMethod(md, ctx, globals);
            else if (member instanceof ClassDeclaration inner) processClass(inner, ctx, globals);
        }
    }

    private static void processMethod(@NotNull MethodDeclaration md, @NotNull TransformContext ctx, @NotNull Map<String, GlobalInfo> globals) {
        BlockStatement body = md.body();
        if (body == null) return;
        flushBlock(body, ctx, globals, true, md);
        scanLambdas(body, ctx, globals);
    }

    private static void scanLambdas(@NotNull Statement s, @NotNull TransformContext ctx, @NotNull Map<String, GlobalInfo> globals) {
        if (s instanceof BlockStatement b) {
            for (Statement child : b.statements()) scanLambdas(child, ctx, globals);
        } else if (s instanceof ExpressionStatement es) {
            scanLambdasInExpression(es.expression(), ctx, globals);
        } else if (s instanceof IfStatement is) {
            scanLambdas(is.thenBranch(), ctx, globals);
            if (is.elseBranch() != null) scanLambdas(is.elseBranch(), ctx, globals);
        } else if (s instanceof WhileStatement ws) {
            scanLambdas(ws.body(), ctx, globals);
        } else if (s instanceof ForStatement fs) {
            scanLambdas(fs.body(), ctx, globals);
        } else if (s instanceof ReturnStatement rs && rs.value() != null) {
            scanLambdasInExpression(rs.value(), ctx, globals);
        }
    }

    private static void scanLambdasInExpression(@NotNull Expression e, @NotNull TransformContext ctx, @NotNull Map<String, GlobalInfo> globals) {
        if (e instanceof LambdaExpression le) {
            if (le.body() instanceof BlockStatement lb) {
                flushBlock(lb, ctx, globals, false, null);
                scanLambdas(lb, ctx, globals);
            }
        } else if (e instanceof MethodCallExpression mc) {
            for (Expression arg : mc.arguments()) scanLambdasInExpression(arg, ctx, globals);
            if (mc.target() != null) scanLambdasInExpression(mc.target(), ctx, globals);
        } else if (e instanceof AssignmentExpression ae) {
            scanLambdasInExpression(ae.value(), ctx, globals);
        }
    }

    private static void flushBlock(@NotNull BlockStatement body, @NotNull TransformContext ctx, @NotNull Map<String, GlobalInfo> globals, boolean isMethodBody, @Nullable MethodDeclaration md) {
        Set<String> mutated = new HashSet<>();
        scanForMutations(body, globals.keySet(), mutated);
        if (mutated.isEmpty()) return;

        List<GlobalInfo> dirty = new ArrayList<>();
        for (String name : globals.keySet()) {
            if (mutated.contains(name)) dirty.add(globals.get(name));
        }

        List<String> writebacks = new ArrayList<>(dirty.size());
        for (GlobalInfo info : dirty) {
            writebacks.add(info.storage + ".set(\"" + info.key + "\", " + info.name + ");");
        }

        List<Integer> anchors = new ArrayList<>();
        collectReturns(body, anchors);

        List<TaggedLine> lines = ctx.lines();
        for (int astLine : anchors) {
            int idx = ctx.indexOfFullSourceLine(astLine);
            if (idx < 0) continue;
            if (alreadyHasWriteback(lines, idx, dirty)) continue;
            ctx.insertLinesBefore(idx, writebacks);
        }

        int endIdx;
        if (isMethodBody && md != null) {
            endIdx = findMethodEndIndex(ctx, md);
        } else {
            endIdx = findBlockEndIndex(ctx, body);
        }
        if (endIdx >= 0 && !alreadyHasWriteback(lines, endIdx, dirty)) {
            ctx.insertLinesBefore(endIdx, writebacks);
        }
    }

    private static int findBlockEndIndex(@NotNull TransformContext ctx, @NotNull BlockStatement body) {
        int startIdx = ctx.indexOfFullSourceLine(body.line());
        if (startIdx < 0) return -1;
        List<TaggedLine> lines = ctx.lines();
        int depth = 0;
        boolean entered = false;
        for (int i = startIdx; i < lines.size(); i++) {
            String code = lines.get(i).code();
            for (int j = 0; j < code.length(); j++) {
                char c = code.charAt(j);
                if (c == '{') {
                    depth++;
                    entered = true;
                } else if (c == '}') {
                    depth--;
                    if (entered && depth == 0) return i;
                }
            }
        }
        return -1;
    }

    private static boolean alreadyHasWriteback(@NotNull List<TaggedLine> lines, int idx, @NotNull List<GlobalInfo> dirty) {
        int probe = idx - 1;
        for (int i = dirty.size() - 1; i >= 0 && probe >= 0; i--, probe--) {
            String code = lines.get(probe).code();
            GlobalInfo info = dirty.get(i);
            if (!code.contains(info.storage + MARKER + info.key + "\"")) return false;
        }
        return true;
    }

    private static int findMethodEndIndex(@NotNull TransformContext ctx, @NotNull MethodDeclaration md) {
        int methodStartIdx = ctx.indexOfFullSourceLine(md.line());
        if (methodStartIdx < 0) return -1;
        List<TaggedLine> lines = ctx.lines();
        int depth = 0;
        boolean entered = false;
        for (int i = methodStartIdx; i < lines.size(); i++) {
            String code = lines.get(i).code();
            for (int j = 0; j < code.length(); j++) {
                char c = code.charAt(j);
                if (c == '{') {
                    depth++;
                    entered = true;
                } else if (c == '}') {
                    depth--;
                    if (entered && depth == 0) return i;
                }
            }
        }
        return -1;
    }

    private static void scanForMutations(@NotNull Statement s, @NotNull Set<String> targets, @NotNull Set<String> mutated) {
        if (s instanceof BlockStatement b) {
            for (Statement child : b.statements()) scanForMutations(child, targets, mutated);
        } else if (s instanceof ExpressionStatement es) {
            scanExpression(es.expression(), targets, mutated);
        } else if (s instanceof IfStatement is) {
            scanForMutations(is.thenBranch(), targets, mutated);
            if (is.elseBranch() != null) scanForMutations(is.elseBranch(), targets, mutated);
        } else if (s instanceof WhileStatement ws) {
            scanForMutations(ws.body(), targets, mutated);
        } else if (s instanceof ForStatement fs) {
            scanForMutations(fs.body(), targets, mutated);
            if (fs.updaters() != null) for (Expression e : fs.updaters()) scanExpression(e, targets, mutated);
        } else if (s instanceof ReturnStatement rs && rs.value() != null) {
            scanExpression(rs.value(), targets, mutated);
        }
    }

    private static void scanExpression(@NotNull Expression e, @NotNull Set<String> targets, @NotNull Set<String> mutated) {
        if (e instanceof AssignmentExpression ae) {
            if (ae.target() instanceof NameExpression ne && targets.contains(ne.name()) && !isStorageReload(ae.value())) {
                mutated.add(ne.name());
            }
            scanExpression(ae.value(), targets, mutated);
        } else if (e instanceof UnaryExpression ue) {
            String op = ue.operator();
            if ("++".equals(op) || "--".equals(op)) {
                if (ue.operand() instanceof NameExpression ne && targets.contains(ne.name())) mutated.add(ne.name());
            }
            scanExpression(ue.operand(), targets, mutated);
        } else if (e instanceof MethodCallExpression mc) {
            Expression target = mc.target();
            if (target != null && unwrap(target) instanceof NameExpression tn && targets.contains(tn.name()) && isMutatingMethod(mc.methodName())) {
                mutated.add(tn.name());
            }
            if (target != null) scanExpression(target, targets, mutated);
            for (Expression arg : mc.arguments()) scanExpression(arg, targets, mutated);
        }
    }

    private static boolean isMutatingMethod(@NotNull String name) {
        return switch (name) {
            case "add", "addAll", "remove", "removeAll", "removeIf", "clear", "put", "putAll", "putIfAbsent", "set", "compute", "computeIfAbsent", "computeIfPresent", "merge", "replace", "replaceAll", "sort", "reverse", "shuffle" -> true;
            default -> false;
        };
    }

    private static boolean isStorageReload(@NotNull Expression e) {
        Expression unwrapped = unwrap(e);
        if (!(unwrapped instanceof MethodCallExpression mc)) return false;
        if (!"get".equals(mc.methodName())) return false;
        if (!(mc.target() instanceof NameExpression tn)) return false;
        return "GlobalVars".equals(tn.name()) || "Storage".equals(tn.name()) || "PersistentVars".equals(tn.name());
    }

    private static @NotNull Expression unwrap(@NotNull Expression e) {
        while (true) {
            if (e instanceof CastExpression ce) {
                e = ce.expression();
                continue;
            }
            if (e instanceof ParenExpression pe) {
                e = pe.expression();
                continue;
            }
            return e;
        }
    }

    private static void collectReturns(@NotNull Statement s, @NotNull List<Integer> out) {
        if (s instanceof ReturnStatement rs) {
            out.add(rs.line());
        } else if (s instanceof BlockStatement b) {
            for (Statement child : b.statements()) collectReturns(child, out);
        } else if (s instanceof IfStatement is) {
            collectReturns(is.thenBranch(), out);
            if (is.elseBranch() != null) collectReturns(is.elseBranch(), out);
        } else if (s instanceof WhileStatement ws) {
            collectReturns(ws.body(), out);
        } else if (s instanceof ForStatement fs) {
            collectReturns(fs.body(), out);
        }
    }

    private record GlobalInfo(@NotNull String name, @NotNull String storage, @NotNull String key) {
    }
}
