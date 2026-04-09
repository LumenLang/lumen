package dev.lumenlang.lumen.plugin.defaults.emit.form;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.emit.EmitContext;
import dev.lumenlang.lumen.api.emit.ScriptToken;
import dev.lumenlang.lumen.api.emit.StatementFormHandler;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.pipeline.codegen.BindingContext;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.language.emit.EmitContextImpl;
import dev.lumenlang.lumen.pipeline.language.exceptions.LumenScriptException;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredExpressionMatch;
import dev.lumenlang.lumen.pipeline.language.resolve.ExprResolver;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.language.typed.Expr;
import dev.lumenlang.lumen.pipeline.language.typed.ExprParser;
import dev.lumenlang.lumen.pipeline.language.validator.VarNameValidator;
import dev.lumenlang.lumen.pipeline.persist.PersistentVars;
import dev.lumenlang.lumen.pipeline.placeholder.PlaceholderExpander;
import dev.lumenlang.lumen.api.type.LumenTypeRegistry;
import dev.lumenlang.lumen.api.type.ObjectType;
import dev.lumenlang.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Statement form handler for inline stored variable loading.
 *
 * <p>Accepts the syntax {@code load name [for [type] scope] with default expr}.
 * This loads a persistent variable into the current scope, creating it with the given
 * default value if it does not yet exist. The variable is backed by {@link PersistentVars}
 * and survives server restarts.
 */
@Registration(order = -1999)
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class LoadStatementForm implements StatementFormHandler {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.emitters().statementForm(this);
    }

    private static boolean isLoadStatement(@NotNull List<Token> t) {
        if (t.size() < 5 || !t.get(0).text().equalsIgnoreCase("load")) return false;
        for (int i = 2; i < t.size() - 1; i++) {
            if (t.get(i).text().equalsIgnoreCase("with") && t.get(i + 1).text().equalsIgnoreCase("default")) return true;
        }
        return false;
    }

    @Override
    public boolean tryHandle(@NotNull List<? extends ScriptToken> tokens, @NotNull EmitContext ctx) {
        List<Token> t = EmitContextImpl.toPipelineTokens(tokens);
        if (!isLoadStatement(t)) return false;
        handleLoad(t, ctx);
        return true;
    }

    private void handleLoad(@NotNull List<Token> t, @NotNull EmitContext ctx) {
        String name = t.get(1).text();
        int idx = 2;
        String scopeVar = null;

        if (idx < t.size() && t.get(idx).text().equalsIgnoreCase("for")) {
            idx++;
            if (idx >= t.size()) {
                throw new LumenScriptException(ctx.line(), ctx.raw(), "Expected scope variable after 'for'. Correct syntax: load " + name + " for <scope> with default ...");
            }
            scopeVar = t.get(idx).text();
            idx++;
        }

        if (idx >= t.size() || !t.get(idx).text().equalsIgnoreCase("with")) {
            throw new LumenScriptException(ctx.line(), ctx.raw(), "Expected 'with default'. Correct syntax: load " + name + " [for <scope>] with default ...");
        }
        idx++;
        if (idx >= t.size() || !t.get(idx).text().equalsIgnoreCase("default")) {
            throw new LumenScriptException(ctx.line(), ctx.raw(), "Expected 'default' after 'with'. Correct syntax: load " + name + " [for <scope>] with default ...");
        }
        idx++;

        List<Token> exprTokens = t.subList(idx, t.size());
        emitLoadVar(name, scopeVar, exprTokens, ctx);
    }

    private void emitLoadVar(@NotNull String name, @Nullable String scopeVar, @NotNull List<Token> exprTokens, @NotNull EmitContext ctx) {
        TypeEnv env = (TypeEnv) ctx.env();

        String nameError = VarNameValidator.validate(name);
        if (nameError != null) {
            throw new LumenScriptException(ctx.line(), ctx.raw(), nameError);
        }

        if (env.lookupVar(name) != null) {
            throw new LumenScriptException(ctx.line(), ctx.raw(), "Variable '" + name + "' is already defined in this scope");
        }

        RegisteredExpressionMatch exprMatch = PatternRegistry.instance().matchExpression(exprTokens, env);

        String defaultJava;
        ObjectType resolvedObjectType = null;

        if (exprMatch != null) {
            BindingContext bc = new BindingContext(exprMatch.match(), env, ((EmitContextImpl) ctx).codegenContext(), env.blockContext());
            ExpressionResult result = exprMatch.reg().handler().handle(bc);
            defaultJava = result.java();
            resolvedObjectType = LumenTypeRegistry.byId(result.typeId());
        } else {
            Expr e = ExprParser.parse(exprTokens, env);
            if (e instanceof Expr.Literal l) {
                defaultJava = l.value() instanceof String s ? PlaceholderExpander.expand(s, env) : l.value().toString();
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
                throw new LumenScriptException(ctx.line(), ctx.raw(), "Expression not recognized: '" + ExprResolver.joinTokens(raw.tokens()) + "'. Check spelling of variables and expression patterns.", raw.tokens());
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
            if (scopeRef.objectType() != null) {
                scopeKeyPart = scopeRef.objectType().keyExpression(scopeRef.java());
            } else {
                scopeKeyPart = "String.valueOf(" + scopeRef.java() + ")";
            }
            keyExpr = "\"" + className + "." + name + ".\" + " + scopeKeyPart;
        } else {
            keyExpr = "\"" + className + "." + name + "\"";
        }

        ctx.codegen().addImport(PersistentVars.class.getName());
        ctx.out().line("var " + name + " = PersistentVars.get(" + keyExpr + ", " + defaultJava + ");");
        VarRef varRef = new VarRef(resolvedObjectType, name);
        env.defineVar(name, varRef);
        if (env.blockContext().parent() != null) {
            env.blockContext().parent().defineVar(name, varRef);
        }
        String baseKey = "\"" + className + "." + name + ".\"";
        ctx.env().markStored(name, keyExpr, baseKey, scopeVar);
    }
}
