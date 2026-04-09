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
import dev.lumenlang.lumen.pipeline.placeholder.PlaceholderExpander;
import dev.lumenlang.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Statement form handler for global variable declarations.
 *
 * <p>Accepts the syntax {@code global [stored] [scoped] name [with default expr]}.
 * Declarations without {@code scoped} create a server-wide global loaded automatically
 * at the start of every block body. Declarations with {@code scoped} create a per-entity
 * global whose storage key includes a runtime identifier derived from the scope variable.
 * Scoped globals are not loaded automatically and must be accessed explicitly via
 * {@code get name for scope} and {@code set name to value for scope}.
 *
 * <p>The {@code with default expr} clause is optional. When omitted, the default is
 * {@code null}.
 *
 * <p>When {@code stored} is present, the variable is persisted to disk and survives
 * server restarts. Without {@code stored}, the value lives only in memory.
 */
@Registration(order = -2000)
@SuppressWarnings("unused")
public final class GlobalDeclarationForm implements StatementFormHandler {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.emitters().statementForm(this);
    }

    private static boolean isGlobalDeclaration(@NotNull List<Token> t) {
        if (t.size() < 2) return false;
        if (!t.get(0).text().equalsIgnoreCase("global")) return false;
        int idx = 1;
        if (t.get(idx).text().equalsIgnoreCase("stored")) idx++;
        if (idx < t.size() && t.get(idx).text().equalsIgnoreCase("scoped")) idx++;
        if (idx >= t.size()) return false;
        String nameCandidate = t.get(idx).text();
        return !nameCandidate.equalsIgnoreCase("with") && !nameCandidate.equalsIgnoreCase("default");
    }

    @Override
    public boolean tryHandle(@NotNull List<? extends ScriptToken> tokens, @NotNull EmitContext ctx) {
        List<Token> t = EmitContextImpl.toPipelineTokens(tokens);
        if (!isGlobalDeclaration(t)) return false;
        handleGlobal(t, ctx);
        return true;
    }

    private void handleGlobal(@NotNull List<Token> t, @NotNull EmitContext ctx) {
        int idx = 1;
        boolean stored = false;
        boolean scoped = false;
        if (idx < t.size() && t.get(idx).text().equalsIgnoreCase("stored")) {
            stored = true;
            idx++;
        }
        if (idx < t.size() && t.get(idx).text().equalsIgnoreCase("scoped")) {
            scoped = true;
            idx++;
        }

        if (idx >= t.size()) {
            throw new LumenScriptException(ctx.line(), ctx.raw(), "Expected variable name. Correct syntax: global [stored] [scoped] <name> [with default <expr>]");
        }

        String name = t.get(idx).text();
        idx++;

        List<Token> exprTokens = null;
        if (idx < t.size()) {
            if (!t.get(idx).text().equalsIgnoreCase("with")) {
                throw new LumenScriptException(ctx.line(), ctx.raw(), "Unexpected token '" + t.get(idx).text() + "'. Correct syntax: global [stored] [scoped] " + name + " [with default <expr>]");
            }
            idx++;
            if (idx >= t.size() || !t.get(idx).text().equalsIgnoreCase("default")) {
                throw new LumenScriptException(ctx.line(), ctx.raw(), "Expected 'default' after 'with'. Correct syntax: global " + name + " with default <expr>");
            }
            idx++;
            if (idx >= t.size()) {
                throw new LumenScriptException(ctx.line(), ctx.raw(), "Expected an expression after 'with default'.");
            }
            exprTokens = t.subList(idx, t.size());
        }

        registerGlobal(name, scoped, stored, exprTokens, ctx);
    }

    private void registerGlobal(@NotNull String name, boolean scoped, boolean stored, @Nullable List<Token> exprTokens, @NotNull EmitContext ctx) {
        TypeEnv env = (TypeEnv) ctx.env();

        String nameError = VarNameValidator.validate(name);
        if (nameError != null) throw new LumenScriptException(ctx.line(), ctx.raw(), nameError);
        if (env.isGlobal(name)) throw new LumenScriptException(ctx.line(), ctx.raw(), "Global variable '" + name + "' is already declared");
        if (env.lookupVar(name) != null) throw new LumenScriptException(ctx.line(), ctx.raw(), "Variable '" + name + "' is already defined in this scope");

        String className = ctx.codegen().className();
        String defaultJava;
        String resolvedObjectTypeId = null;
        Map<String, Object> exprMetadata = null;

        if (exprTokens == null) {
            env.registerGlobal(new TypeEnv.GlobalVarInfo(name, "(Object) null", className, scoped, null, stored, null));
            return;
        }

        Expr expr = ExprParser.parse(exprTokens, env);
        if (!(expr instanceof Expr.RawExpr)) {
            defaultJava = resolveDefaultJava(expr, env);
        } else {
            RegisteredExpressionMatch exprMatch = PatternRegistry.instance().matchExpression(exprTokens, env);
            if (exprMatch != null) {
                BindingContext bc = new BindingContext(exprMatch.match(), env, ((EmitContextImpl) ctx).codegenContext(), env.blockContext());
                ExpressionResult result = exprMatch.reg().handler().handle(bc);
                defaultJava = result.java();
                resolvedObjectTypeId = result.typeId();
                exprMetadata = result.metadata();
            } else {
                throw new LumenScriptException(ctx.line(), ctx.raw(), "Expression not recognized: '" + ExprResolver.joinTokens(((Expr.RawExpr) expr).tokens()) + "'. Check spelling of variables and expression patterns.", ((Expr.RawExpr) expr).tokens());
            }
        }

        env.registerGlobal(new TypeEnv.GlobalVarInfo(name, defaultJava, className, scoped, resolvedObjectTypeId, stored, exprMetadata));
    }

    private @NotNull String resolveDefaultJava(@NotNull Expr expr, @NotNull TypeEnv env) {
        if (expr instanceof Expr.Literal l) {
            if (l.value() == null) return "(Object) null";
            return l.value() instanceof String s ? PlaceholderExpander.expand(s, env) : l.value().toString();
        }
        if (expr instanceof Expr.RefExpr r) {
            VarRef ref = env.lookupVar(r.name());
            if (ref == null) throw new RuntimeException("Variable not found: " + r.name());
            return ref.java();
        }
        if (expr instanceof Expr.MathExpr m) return m.java();
        throw new IllegalStateException("Unexpected expression type: " + expr.getClass().getSimpleName());
    }
}
