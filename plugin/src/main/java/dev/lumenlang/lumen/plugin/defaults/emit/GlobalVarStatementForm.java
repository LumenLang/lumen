package dev.lumenlang.lumen.plugin.defaults.emit;

import dev.lumenlang.lumen.api.emit.EmitContext;
import dev.lumenlang.lumen.api.emit.ScriptToken;
import dev.lumenlang.lumen.api.emit.StatementFormHandler;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.pipeline.codegen.BindingContext;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.language.emit.EmitContextImpl;
import dev.lumenlang.lumen.pipeline.language.exceptions.LumenScriptException;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.pattern.RegisteredExpressionMatch;
import dev.lumenlang.lumen.pipeline.language.resolve.ExprResolver;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.language.typed.Expr;
import dev.lumenlang.lumen.pipeline.language.typed.ExprParser;
import dev.lumenlang.lumen.pipeline.language.validator.VarNameValidator;
import dev.lumenlang.lumen.pipeline.placeholder.PlaceholderExpander;
import dev.lumenlang.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Statement form handler for global variable declarations.
 *
 * <p>Handles both the new syntax ({@code global [stored] var x [for [ref type] refType] default expr})
 * and the shorthand syntax ({@code global x [for refType] default expr}).
 */
@SuppressWarnings("DataFlowIssue")
public final class GlobalVarStatementForm implements StatementFormHandler {

    private static boolean isNewGlobalStatement(@NotNull List<Token> t) {
        if (t.size() < 5 || !t.get(0).text().equalsIgnoreCase("global")) {
            return false;
        }
        int idx = 1;
        if (t.get(idx).text().equalsIgnoreCase("stored")) {
            idx++;
        }
        if (!t.get(idx).text().equalsIgnoreCase("var")) {
            return false;
        }
        String nameCandidate = t.get(idx + 1).text();
        if (nameCandidate.equalsIgnoreCase("for") || nameCandidate.equalsIgnoreCase("default")) {
            return false;
        }
        idx += 2;
        for (int i = idx; i < t.size(); i++) {
            if (t.get(i).text().equalsIgnoreCase("default")) return true;
        }
        return false;
    }

    private static boolean isShorthandGlobalStatement(@NotNull List<Token> t) {
        if (t.size() < 4 || !t.get(0).text().equalsIgnoreCase("global")) {
            return false;
        }
        if (t.get(2).text().equalsIgnoreCase("default")) return true;
        return t.size() >= 6
                && t.get(2).text().equalsIgnoreCase("for")
                && t.get(4).text().equalsIgnoreCase("default");
    }

    @Override
    public boolean tryHandle(@NotNull List<? extends ScriptToken> tokens, @NotNull EmitContext ctx) {
        List<Token> t = EmitContextImpl.toPipelineTokens(tokens);

        if (isNewGlobalStatement(t)) {
            handleNewGlobal(t, ctx);
            return true;
        }
        if (isShorthandGlobalStatement(t)) {
            handleShorthandGlobal(t, ctx);
            return true;
        }
        return false;
    }

    private void handleNewGlobal(@NotNull List<Token> t, @NotNull EmitContext ctx) {
        int idx = 1;
        boolean stored = false;
        if (t.get(idx).text().equalsIgnoreCase("stored")) {
            stored = true;
            idx++;
        }
        idx++;

        String name = t.get(idx).text();
        idx++;

        String refTypeName = null;
        if (idx < t.size() && t.get(idx).text().equalsIgnoreCase("for")) {
            idx++;
            if (idx + 1 < t.size()
                    && t.get(idx).text().equalsIgnoreCase("ref")
                    && t.get(idx + 1).text().equalsIgnoreCase("type")) {
                idx += 2;
            }
            refTypeName = t.get(idx).text();
            idx++;
        }

        idx++;

        List<Token> exprTokens = t.subList(idx, t.size());
        registerGlobal(name, refTypeName, exprTokens, stored, ctx);
    }

    private void handleShorthandGlobal(@NotNull List<Token> t, @NotNull EmitContext ctx) {
        String name = t.get(1).text();
        String refTypeName = null;
        int defaultIdx;

        if (t.get(2).text().equalsIgnoreCase("for")) {
            refTypeName = t.get(3).text();
            defaultIdx = 4;
        } else {
            defaultIdx = 2;
        }

        List<Token> exprTokens = t.subList(defaultIdx + 1, t.size());
        registerGlobal(name, refTypeName, exprTokens, true, ctx);
    }

    private void registerGlobal(@NotNull String name, @Nullable String refTypeName,
                                @NotNull List<Token> exprTokens, boolean stored,
                                @NotNull EmitContext ctx) {
        TypeEnv env = (TypeEnv) ctx.env();

        String nameError = VarNameValidator.validate(name);
        if (nameError != null) {
            throw new LumenScriptException(ctx.line(), ctx.raw(), nameError);
        }

        String className = ctx.codegen().className();
        String normalizedRefType = refTypeName != null
                ? refTypeName.toUpperCase(Locale.ROOT)
                : null;

        String defaultJava;
        String exprRefTypeId = null;
        Map<String, Object> exprMetadata = null;

        Expr expr = ExprParser.parse(exprTokens, env);
        if (!(expr instanceof Expr.RawExpr)) {
            defaultJava = resolveDefaultJava(expr, env);
        } else {
            RegisteredExpressionMatch exprMatch = PatternRegistry.instance().matchExpression(exprTokens, env);
            if (exprMatch != null) {
                BindingContext bc = new BindingContext(
                        exprMatch.match(), env,
                        ((EmitContextImpl) ctx).codegenContext(),
                        env.blockContext());
                ExpressionResult result = exprMatch.reg().handler().handle(bc);
                defaultJava = result.java();
                exprRefTypeId = result.refTypeId();
                exprMetadata = result.metadata();
            } else {
                throw new LumenScriptException(ctx.line(), ctx.raw(),
                        "Expression not recognized: '" + ExprResolver.joinTokens(((Expr.RawExpr) expr).tokens())
                                + "'. Check spelling of variables and expression patterns.",
                        ((Expr.RawExpr) expr).tokens());
            }
        }

        env.registerGlobal(new TypeEnv.GlobalVarInfo(
                name, defaultJava, className, normalizedRefType,
                exprRefTypeId, stored, exprMetadata));
    }

    private @NotNull String resolveDefaultJava(@NotNull Expr expr, @NotNull TypeEnv env) {
        if (expr instanceof Expr.Literal l) {
            return l.value() instanceof String s
                    ? PlaceholderExpander.expand(s, env)
                    : l.value().toString();
        }
        if (expr instanceof Expr.RefExpr r) {
            VarRef ref = env.lookupVar(r.name());
            if (ref == null) {
                throw new RuntimeException("Variable not found: " + r.name());
            }
            return ref.java();
        }
        if (expr instanceof Expr.MathExpr m) {
            return m.java();
        }
        throw new IllegalStateException("Unexpected expression type: " + expr.getClass().getSimpleName());
    }
}
