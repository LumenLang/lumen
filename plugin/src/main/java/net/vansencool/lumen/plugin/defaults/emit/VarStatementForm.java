package net.vansencool.lumen.plugin.defaults.emit;

import net.vansencool.lumen.api.emit.EmitContext;
import net.vansencool.lumen.api.emit.ScriptToken;
import net.vansencool.lumen.api.emit.StatementFormHandler;
import net.vansencool.lumen.api.handler.ExpressionHandler.ExpressionResult;
import net.vansencool.lumen.pipeline.codegen.BindingContext;
import net.vansencool.lumen.pipeline.codegen.BlockContext;
import net.vansencool.lumen.pipeline.codegen.TypeEnv;
import net.vansencool.lumen.pipeline.language.emit.EmitContextImpl;
import net.vansencool.lumen.pipeline.language.exceptions.LumenScriptException;
import net.vansencool.lumen.pipeline.language.pattern.PatternRegistry;
import net.vansencool.lumen.pipeline.language.pattern.RegisteredExpressionMatch;
import net.vansencool.lumen.pipeline.language.resolve.ExprResolver;
import net.vansencool.lumen.pipeline.language.tokenization.Token;
import net.vansencool.lumen.pipeline.language.tokenization.TokenKind;
import net.vansencool.lumen.pipeline.language.typed.Expr;
import net.vansencool.lumen.pipeline.language.typed.ExprParser;
import net.vansencool.lumen.pipeline.language.validator.VarNameValidator;
import net.vansencool.lumen.pipeline.placeholder.PlaceholderExpander;
import net.vansencool.lumen.pipeline.var.RefType;
import net.vansencool.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Statement form handler for {@code var x = <expr>} declarations.
 *
 * <p>Handles plain variable assignment where the RHS is a literal, reference,
 * math expression, or resolvable expression. For multi-token expressions, it
 * first tries matching against registered expression patterns (preserving the
 * refType from the {@link ExpressionResult}), then falls back to
 * {@link ExprResolver} for nested sub-expressions and arithmetic.
 */
@SuppressWarnings("DataFlowIssue")
public final class VarStatementForm implements StatementFormHandler {

    @Override
    public boolean tryHandle(@NotNull List<? extends ScriptToken> tokens, @NotNull EmitContext ctx) {
        if (tokens.size() < 4
                || !tokens.get(0).text().equalsIgnoreCase("var")
                || !tokens.get(2).text().equals("=")) {
            return false;
        }

        List<Token> pipelineTokens = EmitContextImpl.toPipelineTokens(tokens);
        String name = pipelineTokens.get(1).text();
        TypeEnv env = (TypeEnv) ctx.env();

        String nameError = VarNameValidator.validate(name);
        if (nameError != null) {
            throw new LumenScriptException(ctx.line(), ctx.raw(), nameError,
                    List.of(pipelineTokens.get(1)));
        }

        if (env.lookupVar(name) != null) {
            throw new LumenScriptException(ctx.line(), ctx.raw(),
                    "Variable '" + name + "' is already defined in this scope");
        }

        if (env.blockContext().isRoot()) {
            throw new LumenScriptException(ctx.line(), ctx.raw(),
                    "'var' cannot be used at the top level of a script. "
                    + "Use 'global var " + name + " default <value>' instead to declare a script-wide variable.",
                    List.of(pipelineTokens.get(0)));
        }

        List<Token> exprTokens = pipelineTokens.subList(3, pipelineTokens.size());
        Expr e = ExprParser.parse(exprTokens, env);

        String java;
        VarRef inheritedRef = null;
        @Nullable RefType resolvedRefType = null;
        @NotNull Map<String, Object> resolvedMetadata = Map.of();

        if (e instanceof Expr.Literal l) {
            if (l.value() instanceof String s) {
                java = PlaceholderExpander.expand(s, env);
            } else if (l.value() instanceof Boolean b) {
                java = b.toString();
            } else {
                java = l.value().toString();
            }
        } else if (e instanceof Expr.RefExpr r) {
            VarRef ref = env.lookupVar(r.name());
            if (ref == null) {
                throw new RuntimeException("Variable not found: " + r.name());
            }
            java = ref.java();
            inheritedRef = ref;
        } else if (e instanceof Expr.MathExpr m) {
            java = m.java();
        } else {
            Expr.RawExpr raw = (Expr.RawExpr) e;
            if (raw.tokens().size() > 1) {
                ExpressionResult exprResult = tryExpressionPattern(raw.tokens(), (EmitContextImpl) ctx, env);
                if (exprResult != null) {
                    java = exprResult.java();
                    if (exprResult.refTypeId() != null) {
                        resolvedRefType = RefType.byId(exprResult.refTypeId());
                    }
                    resolvedMetadata = exprResult.metadata();
                } else {
                    String resolved = ExprResolver.resolve(
                            raw.tokens(),
                            ((EmitContextImpl) ctx).codegenContext(),
                            env);
                    if (resolved != null) {
                        java = resolved;
                    } else {
                        return false;
                    }
                }
            } else {
                Token singleToken = raw.tokens().get(0);
                if (singleToken.kind() == TokenKind.IDENT && env.lookupVar(singleToken.text()) == null) {
                    throw new LumenScriptException(ctx.line(), ctx.raw(),
                            "Variable '" + singleToken.text() + "' does not exist. Did you mean to define it first?",
                            List.of(singleToken));
                }
                java = ExprResolver.joinTokens(raw.tokens());
            }
        }

        ctx.out().line("var " + name + " = " + java + ";");
        RefType effectiveRefType = inheritedRef != null ? inheritedRef.refType() : resolvedRefType;
        VarRef varRef = inheritedRef != null
                ? new VarRef(effectiveRefType, name)
                : new VarRef(effectiveRefType, name, resolvedMetadata);
        env.defineVar(name, varRef);
        if (env.blockContext().parent() != null) {
            env.blockContext().parent().defineVar(name, varRef);
        }
        return true;
    }

    /**
     * Tries to match the given tokens against a registered expression pattern,
     * returning the full {@link ExpressionResult} (including refType) on success.
     *
     * @param tokens the expression tokens to match
     * @param ctx    the emit context
     * @param env    the type environment
     * @return the expression result with refType preserved, or null if no pattern matched
     */
    private static @Nullable ExpressionResult tryExpressionPattern(
            @NotNull List<Token> tokens,
            @NotNull EmitContextImpl ctx,
            @NotNull TypeEnv env) {
        PatternRegistry reg;
        try {
            reg = PatternRegistry.instance();
        } catch (RuntimeException e) {
            return null;
        }
        RegisteredExpressionMatch match = reg.matchExpression(tokens, env);
        if (match == null) {
            return null;
        }
        try {
            BlockContext block = env.blockContext();
            BindingContext bc = new BindingContext(match.match(), env, ctx.codegenContext(), block);
            return match.reg().handler().handle(bc);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
