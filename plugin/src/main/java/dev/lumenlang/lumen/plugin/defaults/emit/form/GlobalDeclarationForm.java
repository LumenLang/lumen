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
import java.util.Locale;
import java.util.Map;

/**
 * Statement form handler for global variable declarations.
 *
 * <p>Accepts the syntax {@code global [stored] name [for type refType] with default expr}
 * and {@code global [stored] name [for ref type refType] with default expr}.
 * Declarations without {@code for type} create a server-wide global loaded automatically
 * at the start of every block body. Declarations with {@code for type} create a scoped
 * global whose storage key includes a runtime identifier derived from the given reference type.
 * Scoped globals are not loaded automatically and must be accessed explicitly via
 * {@code get name for scope} and {@code set name to value for scope}.
 *
 * <p>When {@code stored} is present, the variable is persisted to disk and survives
 * server restarts. Without {@code stored}, the value lives only in memory.
 */
@Registration(order = -2000)
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class GlobalDeclarationForm implements StatementFormHandler {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.emitters().statementForm(this);
    }

    private static boolean isGlobalDeclaration(@NotNull List<Token> t) {
        if (t.size() < 5) return false;
        if (!t.get(0).text().equalsIgnoreCase("global")) return false;
        int start = 1;
        if (t.get(start).text().equalsIgnoreCase("stored")) start++;
        String nameCandidate = t.get(start).text();
        if (nameCandidate.equalsIgnoreCase("for") || nameCandidate.equalsIgnoreCase("with") || nameCandidate.equalsIgnoreCase("default")) return false;
        for (int i = start + 1; i < t.size() - 1; i++) {
            if (t.get(i).text().equalsIgnoreCase("with") && t.get(i + 1).text().equalsIgnoreCase("default")) return true;
        }
        return false;
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
        if (t.get(idx).text().equalsIgnoreCase("stored")) {
            stored = true;
            idx++;
        }

        String name = t.get(idx).text();
        idx++;

        String refTypeName = null;
        if (idx < t.size() && t.get(idx).text().equalsIgnoreCase("for")) {
            idx++;
            if (idx < t.size() && t.get(idx).text().equalsIgnoreCase("ref")) {
                idx++;
                if (idx >= t.size() || !t.get(idx).text().equalsIgnoreCase("type")) {
                    throw new LumenScriptException(ctx.line(), ctx.raw(), "Expected 'type' after 'ref'. Correct syntax: global " + name + " for ref type <refType> with default ...");
                }
                idx++;
            } else if (idx < t.size() && t.get(idx).text().equalsIgnoreCase("type")) {
                idx++;
            } else {
                throw new LumenScriptException(ctx.line(), ctx.raw(), "Expected 'type' or 'ref type' after 'for'. Correct syntax: global " + name + " for type <refType> with default ...");
            }
            if (idx >= t.size()) {
                throw new LumenScriptException(ctx.line(), ctx.raw(), "Expected ref type name after 'for type'. Correct syntax: global " + name + " for type <refType> with default ...");
            }
            refTypeName = t.get(idx).text().toUpperCase(Locale.ROOT);
            idx++;
        }

        if (idx >= t.size() || !t.get(idx).text().equalsIgnoreCase("with")) {
            throw new LumenScriptException(ctx.line(), ctx.raw(), "Expected 'with default'. Correct syntax: global " + name + " with default ...");
        }
        idx++;
        if (idx >= t.size() || !t.get(idx).text().equalsIgnoreCase("default")) {
            throw new LumenScriptException(ctx.line(), ctx.raw(), "Expected 'default' after 'with'. Correct syntax: global " + name + " with default ...");
        }
        idx++;

        registerGlobal(name, refTypeName, stored, t.subList(idx, t.size()), ctx);
    }

    private void registerGlobal(@NotNull String name, @Nullable String refTypeName, boolean stored, @NotNull List<Token> exprTokens, @NotNull EmitContext ctx) {
        TypeEnv env = (TypeEnv) ctx.env();

        String nameError = VarNameValidator.validate(name);
        if (nameError != null) throw new LumenScriptException(ctx.line(), ctx.raw(), nameError);
        if (env.isGlobal(name)) throw new LumenScriptException(ctx.line(), ctx.raw(), "Global variable '" + name + "' is already declared");
        if (env.lookupVar(name) != null) throw new LumenScriptException(ctx.line(), ctx.raw(), "Variable '" + name + "' is already defined in this scope");

        String className = ctx.codegen().className();
        String defaultJava;
        String exprRefTypeId = null;
        Map<String, Object> exprMetadata = null;

        Expr expr = ExprParser.parse(exprTokens, env);
        if (!(expr instanceof Expr.RawExpr)) {
            defaultJava = resolveDefaultJava(expr, env);
        } else {
            RegisteredExpressionMatch exprMatch = PatternRegistry.instance().matchExpression(exprTokens, env);
            if (exprMatch != null) {
                BindingContext bc = new BindingContext(exprMatch.match(), env, ((EmitContextImpl) ctx).codegenContext(), env.blockContext());
                ExpressionResult result = exprMatch.reg().handler().handle(bc);
                defaultJava = result.java();
                exprRefTypeId = result.refTypeId();
                exprMetadata = result.metadata();
            } else {
                throw new LumenScriptException(ctx.line(), ctx.raw(), "Expression not recognized: '" + ExprResolver.joinTokens(((Expr.RawExpr) expr).tokens()) + "'. Check spelling of variables and expression patterns.", ((Expr.RawExpr) expr).tokens());
            }
        }

        env.registerGlobal(new TypeEnv.GlobalVarInfo(name, defaultJava, className, refTypeName, exprRefTypeId, stored, exprMetadata));
    }

    private @NotNull String resolveDefaultJava(@NotNull Expr expr, @NotNull TypeEnv env) {
        if (expr instanceof Expr.Literal l) {
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
