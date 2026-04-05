package dev.lumenlang.lumen.plugin.defaults.emit.form;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.emit.EmitContext;
import dev.lumenlang.lumen.api.emit.ScriptToken;
import dev.lumenlang.lumen.api.emit.StatementFormHandler;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.pipeline.codegen.BindingContext;
import dev.lumenlang.lumen.pipeline.codegen.BlockContext;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.language.emit.EmitContextImpl;
import dev.lumenlang.lumen.pipeline.language.exceptions.LumenScriptException;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredExpressionMatch;
import dev.lumenlang.lumen.pipeline.language.resolve.ExprResolver;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.language.tokenization.TokenKind;
import dev.lumenlang.lumen.pipeline.language.typed.Expr;
import dev.lumenlang.lumen.pipeline.language.typed.ExprParser;
import dev.lumenlang.lumen.pipeline.language.validator.VarNameValidator;
import dev.lumenlang.lumen.pipeline.placeholder.PlaceholderExpander;
import dev.lumenlang.lumen.pipeline.type.LumenType;
import dev.lumenlang.lumen.pipeline.var.RefType;
import dev.lumenlang.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Handles all {@code set x to <expr>} statements, including local variable
 * declaration, reassignment, and scoped global operations.
 */
@Registration(order = -1998)
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class VarDeclarationForm implements StatementFormHandler {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.emitters().statementForm(this);
    }

    @Override
    public boolean tryHandle(@NotNull List<? extends ScriptToken> tokens, @NotNull EmitContext ctx) {
        if (tokens.size() < 4
                || !tokens.get(0).text().equalsIgnoreCase("set")
                || !tokens.get(2).text().equalsIgnoreCase("to")) {
            return false;
        }

        List<Token> pipelineTokens = EmitContextImpl.toPipelineTokens(tokens);
        String name = pipelineTokens.get(1).text();
        TypeEnv env = (TypeEnv) ctx.env();
        EmitContextImpl emitCtx = (EmitContextImpl) ctx;
        List<Token> exprTokens = pipelineTokens.subList(3, pipelineTokens.size());

        EnvironmentAccess.GlobalInfo globalInfo = env.getGlobalInfo(name);
        if (globalInfo != null && !env.isGlobalField(name)) {
            emitScopedGlobalSet(name, globalInfo, exprTokens, emitCtx, env);
            return true;
        }

        VarRef existing = env.lookupVar(name);
        if (existing != null) {
            if (tryReassignment(name, existing, exprTokens, pipelineTokens, emitCtx, env)) {
                return true;
            }
            if (globalInfo != null) {
                emitScopedGlobalSet(name, globalInfo, exprTokens, emitCtx, env);
                return true;
            }
            throw new LumenScriptException(emitCtx.line(), emitCtx.raw(), "Cannot resolve expression '" + ExprResolver.joinTokens(exprTokens) + "'.", exprTokens);
        }

        emitDeclaration(name, exprTokens, pipelineTokens, emitCtx, env);
        return true;
    }

    private static void emitScopedGlobalSet(@NotNull String name, @NotNull EnvironmentAccess.GlobalInfo info, @NotNull List<Token> exprTokens, @NotNull EmitContextImpl ctx, @NotNull TypeEnv env) {
        if (!info.scoped()) {
            throw new LumenScriptException(ctx.line(), ctx.raw(), "Variable '" + name + "' is not a scoped global. Declare it with 'global scoped " + name + "' to use per-entity access.", exprTokens);
        }
        int splitIdx = -1;
        for (int i = exprTokens.size() - 2; i >= 1; i--) {
            String text = exprTokens.get(i).text();
            if (text.equalsIgnoreCase("for") || text.equalsIgnoreCase("of")) {
                splitIdx = i;
                break;
            }
        }
        if (splitIdx < 0) {
            throw new LumenScriptException(ctx.line(), ctx.raw(), "Scoped global '" + name + "' requires a scope (e.g. 'set " + name + " to <value> for <scope>').", exprTokens);
        }

        List<Token> valueTokens = exprTokens.subList(0, splitIdx);
        List<Token> scopeTokens = exprTokens.subList(splitIdx + 1, exprTokens.size());

        String valueJava = resolveExpressionJava(valueTokens, ctx, env);
        if (valueJava == null) {
            throw new LumenScriptException(ctx.line(), ctx.raw(), "Cannot resolve value expression '" + ExprResolver.joinTokens(valueTokens) + "'.", valueTokens);
        }

        if (scopeTokens.size() != 1) {
            throw new LumenScriptException(ctx.line(), ctx.raw(), "Invalid scope expression. Expected a single variable name after 'for'.", scopeTokens);
        }
        String scopeVarName = scopeTokens.get(0).text();
        VarRef scopeRef = env.lookupVar(scopeVarName);
        if (scopeRef == null) {
            throw new LumenScriptException(ctx.line(), ctx.raw(), "Scope variable '" + scopeVarName + "' not found.", scopeTokens);
        }
        if (scopeRef.refType() == null) {
            throw new LumenScriptException(ctx.line(), ctx.raw(), "Scope variable '" + scopeVarName + "' has no ref type. Expected a typed variable like a player or entity.", scopeTokens);
        }

        String storageClass = info.stored() ? "PersistentVars" : "GlobalVars";
        String keyExpr = "\"" + info.className() + "." + name + ".\" + " + scopeRef.refType().keyExpression(scopeRef.java());
        ctx.out().line(storageClass + ".set(" + keyExpr + ", " + valueJava + ");");
        VarRef fieldRef = env.lookupVar(name);
        if (fieldRef != null) {
            ctx.out().line(fieldRef.java() + " = " + valueJava + ";");
        }
    }

    private static boolean tryReassignment(@NotNull String name, @NotNull VarRef ref, @NotNull List<Token> exprTokens, @NotNull List<Token> pipelineTokens, @NotNull EmitContextImpl ctx, @NotNull TypeEnv env) {
        BlockContext block = env.blockContext();
        if (block.getEnvFromParents("__lambda_block") != null && env.isVarCapturedByLambda(name)) {
            throw new LumenScriptException(ctx.line(), ctx.raw(), "Cannot modify '" + name + "' inside a schedule block. Use 'global " + name + " with default <value>' instead.", List.of(pipelineTokens.get(1)));
        }
        String java = resolveExpressionJava(exprTokens, ctx, env);
        if (java == null) {
            return false;
        }
        ctx.out().line(ref.java() + " = Coerce.coerce(" + java + ", " + ref.java() + ");");
        if (env.isStored(name)) {
            ctx.out().line(env.storedClassName(name) + ".set(" + env.getStoredKey(name) + ", " + ref.java() + ");");
        }
        return true;
    }

    private static void emitDeclaration(@NotNull String name, @NotNull List<Token> exprTokens, @NotNull List<Token> pipelineTokens, @NotNull EmitContextImpl ctx, @NotNull TypeEnv env) {
        String nameError = VarNameValidator.validate(name);
        if (nameError != null) {
            throw new LumenScriptException(ctx.line(), ctx.raw(), nameError, List.of(pipelineTokens.get(1)));
        }

        if (env.blockContext().isRoot()) {
            throw new LumenScriptException(ctx.line(), ctx.raw(), "'set' cannot be used at the top level of a script. Use 'global " + name + " with default <value>' instead.", List.of(pipelineTokens.get(0)));
        }

        Expr e = ExprParser.parse(exprTokens, env);

        String java;
        VarRef inheritedRef = null;
        @Nullable RefType resolvedRefType = null;
        @Nullable LumenType exprLumenType = null;
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
            resolvedMetadata = ref.metadata();
        } else if (e instanceof Expr.MathExpr m) {
            java = m.java();
        } else {
            Expr.RawExpr raw = (Expr.RawExpr) e;
            if (raw.tokens().size() > 1) {
                ExpressionResult exprResult = tryExpressionPattern(raw.tokens(), ctx, env);
                if (exprResult != null) {
                    java = exprResult.java();
                    if (exprResult.refTypeId() != null) {
                        resolvedRefType = RefType.byId(exprResult.refTypeId());
                    }
                    exprLumenType = LumenType.resolve(exprResult.refTypeId(), exprResult.javaType());
                    resolvedMetadata = exprResult.metadata();
                } else {
                    ExpressionResult resolvedResult = ExprResolver.resolveWithType(raw.tokens(), ctx.codegenContext(), env);
                    if (resolvedResult != null) {
                        java = resolvedResult.java();
                        if (resolvedResult.refTypeId() != null) {
                            resolvedRefType = RefType.byId(resolvedResult.refTypeId());
                        }
                        exprLumenType = LumenType.resolve(resolvedResult.refTypeId(), resolvedResult.javaType());
                        resolvedMetadata = resolvedResult.metadata();
                    } else {
                        throw new LumenScriptException(ctx.line(), ctx.raw(), "Cannot resolve expression '" + ExprResolver.joinTokens(raw.tokens()) + "'.", raw.tokens());
                    }
                }
            } else {
                Token singleToken = raw.tokens().get(0);
                if (singleToken.kind() == TokenKind.IDENT) {
                    if (isNullKeyword(singleToken.text())) {
                        java = "(Object) null";
                    } else if (env.lookupVar(singleToken.text()) == null) {
                        throw new LumenScriptException(ctx.line(), ctx.raw(), "Variable '" + singleToken.text() + "' does not exist. Did you mean to define it first?", List.of(singleToken));
                    } else {
                        java = ExprResolver.joinTokens(raw.tokens());
                    }
                } else {
                    java = ExprResolver.joinTokens(raw.tokens());
                }
            }
        }

        ctx.out().line("var " + name + " = " + java + ";");
        RefType effectiveRefType = inheritedRef != null ? inheritedRef.refType() : resolvedRefType;
        LumenType resolvedLumenType = inheritedRef != null ? inheritedRef.resolvedType() : (exprLumenType != null ? exprLumenType : e.resolvedType());
        if (resolvedLumenType == null && effectiveRefType != null) {
            resolvedLumenType = new LumenType.ObjectType(effectiveRefType);
        }
        VarRef varRef = new VarRef(effectiveRefType, name, resolvedLumenType, resolvedMetadata);
        env.defineVar(name, varRef);
        if (env.blockContext().parent() != null) {
            env.blockContext().parent().defineVar(name, varRef);
        }
    }

    private static boolean isNullKeyword(@NotNull String text) {
        return text.equalsIgnoreCase("none") || text.equalsIgnoreCase("null");
    }

    private static @Nullable ExpressionResult tryExpressionPattern(@NotNull List<Token> tokens, @NotNull EmitContextImpl ctx, @NotNull TypeEnv env) {
        PatternRegistry reg;
        try {
            reg = PatternRegistry.instance();
        } catch (RuntimeException e) {
            return null;
        }
        RegisteredExpressionMatch match = reg.matchExpression(tokens, env);
        if (match == null) {
            match = reg.matchExpressionSlow(tokens, env);
        }
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

    private static @Nullable String resolveExpressionJava(@NotNull List<Token> tokens, @NotNull EmitContextImpl ctx, @NotNull TypeEnv env) {
        Expr e = ExprParser.parse(tokens, env);
        if (e instanceof Expr.Literal l) {
            if (l.value() instanceof String s) return PlaceholderExpander.expand(s, env);
            if (l.value() instanceof Boolean b) return b.toString();
            return l.value().toString();
        }
        if (e instanceof Expr.RefExpr r) {
            VarRef varRef = env.lookupVar(r.name());
            return varRef != null ? varRef.java() : null;
        }
        if (e instanceof Expr.MathExpr m) return m.java();
        Expr.RawExpr raw = (Expr.RawExpr) e;
        if (raw.tokens().size() > 1) {
            ExpressionResult result = tryExpressionPattern(raw.tokens(), ctx, env);
            if (result != null) return result.java();
            ExpressionResult resolved = ExprResolver.resolveWithType(raw.tokens(), ctx.codegenContext(), env);
            return resolved != null ? resolved.java() : null;
        }
        Token single = raw.tokens().get(0);
        if (single.kind() == TokenKind.IDENT) {
            if (isNullKeyword(single.text())) return "null";
            VarRef varRef = env.lookupVar(single.text());
            return varRef != null ? varRef.java() : null;
        }
        return ExprResolver.joinTokens(raw.tokens());
    }
}
