package net.vansencool.lumen.plugin.defaults.emit;

import net.vansencool.lumen.api.emit.EmitContext;
import net.vansencool.lumen.api.emit.ScriptToken;
import net.vansencool.lumen.api.emit.StatementFormHandler;
import net.vansencool.lumen.api.handler.ExpressionHandler.ExpressionResult;
import net.vansencool.lumen.pipeline.codegen.BindingContext;
import net.vansencool.lumen.pipeline.codegen.TypeEnv;
import net.vansencool.lumen.pipeline.java.compiled.NullGuard;
import net.vansencool.lumen.pipeline.language.emit.EmitContextImpl;
import net.vansencool.lumen.pipeline.language.exceptions.LumenScriptException;
import net.vansencool.lumen.pipeline.language.pattern.PatternRegistry;
import net.vansencool.lumen.pipeline.language.pattern.RegisteredExpressionMatch;
import net.vansencool.lumen.pipeline.language.resolve.ExprResolver;
import net.vansencool.lumen.pipeline.language.tokenization.Token;
import net.vansencool.lumen.pipeline.language.typed.Expr;
import net.vansencool.lumen.pipeline.language.typed.ExprParser;
import net.vansencool.lumen.pipeline.language.validator.VarNameValidator;
import net.vansencool.lumen.pipeline.persist.PersistentVars;
import net.vansencool.lumen.pipeline.placeholder.PlaceholderExpander;
import net.vansencool.lumen.pipeline.var.RefType;
import net.vansencool.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Statement form handler for stored variable declarations.
 *
 * <p>Handles both the {@code stored var x [for [ref type] scope] default expr}
 * syntax and the {@code store x [for scope] default expr} syntax.
 */
@SuppressWarnings("DataFlowIssue")
public final class StoreVarStatementForm implements StatementFormHandler {

    @Override
    public boolean tryHandle(@NotNull List<? extends ScriptToken> tokens, @NotNull EmitContext ctx) {
        List<Token> t = EmitContextImpl.toPipelineTokens(tokens);

        if (isStoredVarStatement(t)) {
            return handleStoredVar(t, ctx);
        }
        if (isStoreStatement(t)) {
            return handleStore(t, ctx);
        }
        return false;
    }

    private boolean handleStoredVar(@NotNull List<Token> t, @NotNull EmitContext ctx) {
        String name = t.get(2).text();
        String scopeVar = null;
        int defaultIdx;

        if (t.get(3).text().equalsIgnoreCase("for")) {
            int forIdx = 4;
            if (forIdx + 1 < t.size()
                    && t.get(forIdx).text().equalsIgnoreCase("ref")
                    && t.get(forIdx + 1).text().equalsIgnoreCase("type")) {
                forIdx += 2;
            }
            scopeVar = t.get(forIdx).text();
            defaultIdx = forIdx + 1;
        } else {
            defaultIdx = 3;
        }

        List<Token> exprTokens = t.subList(defaultIdx + 1, t.size());
        return emitStoreVar(name, scopeVar, exprTokens, ctx);
    }

    private boolean handleStore(@NotNull List<Token> t, @NotNull EmitContext ctx) {
        String name = t.get(1).text();
        String scopeVar = null;
        int defaultIdx;

        if (t.get(2).text().equalsIgnoreCase("for")) {
            scopeVar = t.get(3).text();
            defaultIdx = 4;
        } else {
            defaultIdx = 2;
        }

        List<Token> exprTokens = t.subList(defaultIdx + 1, t.size());
        return emitStoreVar(name, scopeVar, exprTokens, ctx);
    }

    private boolean emitStoreVar(@NotNull String name, @Nullable String scopeVar,
                                 @NotNull List<Token> exprTokens, @NotNull EmitContext ctx) {
        TypeEnv env = (TypeEnv) ctx.env();

        String nameError = VarNameValidator.validate(name);
        if (nameError != null) {
            throw new LumenScriptException(ctx.line(), ctx.raw(), nameError);
        }

        if (env.lookupVar(name) != null) {
            throw new LumenScriptException(ctx.line(), ctx.raw(),
                    "Variable '" + name + "' is already defined in this scope");
        }

        RegisteredExpressionMatch exprMatch = PatternRegistry.instance().matchExpression(exprTokens, env);

        String defaultJava;
        RefType exprRefType = null;

        if (exprMatch != null) {
            BindingContext bc = new BindingContext(
                    exprMatch.match(), env,
                    ((EmitContextImpl) ctx).codegenContext(),
                    env.blockContext());
            ExpressionResult result = exprMatch.reg().handler().handle(bc);
            defaultJava = result.java();
            if (result.refTypeId() != null) {
                exprRefType = RefType.byId(result.refTypeId());
            }
        } else {
            Expr e = ExprParser.parse(exprTokens, env);
            if (e instanceof Expr.Literal l) {
                defaultJava = l.value() instanceof String s
                        ? PlaceholderExpander.expand(s, env)
                        : l.value().toString();
            } else if (e instanceof Expr.RefExpr r) {
                VarRef ref = env.lookupVar(r.name());
                if (ref == null) {
                    throw new RuntimeException("Variable not found: " + r.name());
                }
                defaultJava = ref.java();
            } else if (e instanceof Expr.MathExpr m) {
                defaultJava = m.java();
            } else {
                Expr.RawExpr raw = (Expr.RawExpr) e;
                throw new LumenScriptException(ctx.line(), ctx.raw(),
                        "Expression not recognized: '" + ExprResolver.joinTokens(raw.tokens())
                                + "'. Check spelling of variables and expression patterns.",
                        raw.tokens());
            }
        }

        String className = ctx.codegen().className();
        String keyExpr;
        if (scopeVar != null) {
            VarRef scopeRef = env.lookupVar(scopeVar);
            if (scopeRef == null) {
                throw new RuntimeException("Scope variable not found: " + scopeVar);
            }
            String scopeKeyPart;
            if (scopeRef.refType() != null) {
                String guardedVar = NullGuard.codegen(scopeRef.java());
                scopeKeyPart = scopeRef.refType().keyExpression(guardedVar);
            } else {
                scopeKeyPart = "String.valueOf(" + scopeRef.java() + ")";
            }
            keyExpr = "\"" + className + "." + name + ".\" + " + scopeKeyPart;
        } else {
            keyExpr = "\"" + className + "." + name + "\"";
        }

        ctx.codegen().addImport(PersistentVars.class.getName());
        ctx.out().line("var " + name + " = PersistentVars.get(" + keyExpr + ", " + defaultJava + ");");
        VarRef varRef = new VarRef(exprRefType, name);
        env.defineVar(name, varRef);
        if (env.blockContext().parent() != null) {
            env.blockContext().parent().defineVar(name, varRef);
        }
        String baseKey = "\"" + className + "." + name + ".\"";
        ctx.env().markStored(name, keyExpr, baseKey, scopeVar);
        return true;
    }

    private static boolean isStoreStatement(@NotNull List<Token> t) {
        if (t.size() < 4 || !t.get(0).text().equalsIgnoreCase("store")) {
            return false;
        }
        for (int i = 2; i < t.size(); i++) {
            if (t.get(i).text().equalsIgnoreCase("default")) return true;
        }
        return false;
    }

    private static boolean isStoredVarStatement(@NotNull List<Token> t) {
        if (t.size() < 5
                || !t.get(0).text().equalsIgnoreCase("stored")
                || !t.get(1).text().equalsIgnoreCase("var")) {
            return false;
        }
        for (int i = 3; i < t.size(); i++) {
            if (t.get(i).text().equalsIgnoreCase("default")) return true;
        }
        return false;
    }
}
