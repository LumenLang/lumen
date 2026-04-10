package dev.lumenlang.lumen.plugin.defaults.emit.form;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.emit.EmitContext;
import dev.lumenlang.lumen.api.emit.ScriptToken;
import dev.lumenlang.lumen.api.emit.StatementFormHandler;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.NullableType;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.pipeline.codegen.BindingContext;
import dev.lumenlang.lumen.pipeline.codegen.BlockContext;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.language.emit.EmitContextImpl;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredExpressionMatch;
import dev.lumenlang.lumen.pipeline.language.resolve.ExprResolver;
import dev.lumenlang.lumen.pipeline.language.resolve.PatternSimulator;
import dev.lumenlang.lumen.pipeline.language.resolve.SuggestionDiagnostics;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.language.tokenization.TokenKind;
import dev.lumenlang.lumen.pipeline.language.typed.Expr;
import dev.lumenlang.lumen.pipeline.language.typed.ExprParser;
import dev.lumenlang.lumen.pipeline.language.validator.VarNameValidator;
import dev.lumenlang.lumen.pipeline.placeholder.PlaceholderExpander;
import dev.lumenlang.lumen.pipeline.type.TypeChecker;
import dev.lumenlang.lumen.pipeline.util.FuzzyMatch;
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
            throw new DiagnosticException(LumenDiagnostic.error("E502", "Cannot resolve expression")
                    .at(emitCtx.line(), emitCtx.raw())
                    .highlight(exprTokens.get(0).start(), exprTokens.get(exprTokens.size() - 1).end())
                    .label("'" + ExprResolver.joinTokens(exprTokens) + "' is not a recognized expression")
                    .help("check spelling or ensure the variable or expression is defined")
                    .build());
        }

        emitDeclaration(name, exprTokens, pipelineTokens, emitCtx, env);
        return true;
    }

    private static void emitScopedGlobalSet(@NotNull String name, @NotNull EnvironmentAccess.GlobalInfo info, @NotNull List<Token> exprTokens, @NotNull EmitContextImpl ctx, @NotNull TypeEnv env) {
        if (!info.scoped()) {
            throw new DiagnosticException(LumenDiagnostic.error("E502", "Variable '" + name + "' is not a scoped global")
                    .at(ctx.line(), ctx.raw())
                    .highlight(exprTokens.get(0).start(), exprTokens.get(exprTokens.size() - 1).end())
                    .label("'" + name + "' is not scoped")
                    .help("declare with 'global scoped " + name + "' to use per-entity access")
                    .build());
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
            throw new DiagnosticException(LumenDiagnostic.error("E502", "Scoped global '" + name + "' requires a scope")
                    .at(ctx.line(), ctx.raw())
                    .highlight(exprTokens.get(0).start(), exprTokens.get(exprTokens.size() - 1).end())
                    .label("missing scope")
                    .help("use 'set " + name + " to <value> for <scope>'")
                    .build());
        }

        List<Token> valueTokens = exprTokens.subList(0, splitIdx);
        List<Token> scopeTokens = exprTokens.subList(splitIdx + 1, exprTokens.size());

        String valueJava = resolveExpressionJava(valueTokens, ctx, env);
        if (valueJava == null) {
            throw new DiagnosticException(buildExpressionDiagnostic(valueTokens, ctx.line(), ctx.raw(), env));
        }

        if (scopeTokens.size() != 1) {
            throw new DiagnosticException(LumenDiagnostic.error("E502", "Invalid scope expression")
                    .at(ctx.line(), ctx.raw())
                    .highlight(scopeTokens.get(0).start(), scopeTokens.get(scopeTokens.size() - 1).end())
                    .label("expected a single variable name after 'for'")
                    .help("provide a scope variable like a player or entity")
                    .build());
        }
        String scopeVarName = scopeTokens.get(0).text();
        VarRef scopeRef = env.lookupVar(scopeVarName);
        if (scopeRef == null) {
            String suggestion = FuzzyMatch.closest(scopeVarName, env.allVisibleVarNames());
            LumenDiagnostic.Builder diagBuilder = LumenDiagnostic.error("E500", "Variable '" + scopeVarName + "' not found")
                    .at(ctx.line(), ctx.raw())
                    .highlight(scopeTokens.get(0).start(), scopeTokens.get(0).end())
                    .label("undefined variable");
            if (suggestion != null) diagBuilder.help("did you mean '" + suggestion + "'?");
            else diagBuilder.help("make sure the variable is defined before using it");
            throw new DiagnosticException(diagBuilder.build());
        }
        if (scopeRef.objectType() == null) {
            throw new DiagnosticException(LumenDiagnostic.error("E502", "Scope variable '" + scopeVarName + "' has no type")
                    .at(ctx.line(), ctx.raw())
                    .highlight(scopeTokens.get(0).start(), scopeTokens.get(0).end())
                    .label("expected a typed variable like a player or entity")
                    .help("use a typed variable as scope")
                    .build());
        }

        String storageClass = info.stored() ? "PersistentVars" : "GlobalVars";
        String keyExpr = "\"" + info.className() + "." + name + ".\" + " + scopeRef.objectType().keyExpression(scopeRef.java());
        ctx.out().line(storageClass + ".set(" + keyExpr + ", " + valueJava + ");");
        VarRef fieldRef = env.lookupVar(name);
        if (fieldRef != null) {
            ctx.out().line(fieldRef.java() + " = " + valueJava + ";");
        }
    }

    private static boolean tryReassignment(@NotNull String name, @NotNull VarRef ref, @NotNull List<Token> exprTokens, @NotNull List<Token> pipelineTokens, @NotNull EmitContextImpl ctx, @NotNull TypeEnv env) {
        BlockContext block = env.blockContext();
        if (block.getEnvFromParents("__lambda_block") != null && env.isVarCapturedByLambda(name)) {
            throw new DiagnosticException(LumenDiagnostic.error("E502", "Cannot modify '" + name + "' inside a schedule block")
                    .at(ctx.line(), ctx.raw())
                    .highlight(pipelineTokens.get(1).start(), pipelineTokens.get(1).end())
                    .label("captured variable cannot be modified")
                    .help("use 'global " + name + " with default <value>' instead")
                    .build());
        }
        boolean isNone = exprTokens.size() == 1 && exprTokens.get(0).kind() == TokenKind.IDENT && isNullKeyword(exprTokens.get(0).text());
        if (isNone) {
            LumenType varType = ref.type();
            if (!(varType instanceof NullableType)) {
                Token noneToken = exprTokens.get(0);
                LumenDiagnostic diag = TypeChecker.checkNullAssignment(varType, name, ctx.line(), ctx.raw(), noneToken.start(), noneToken.end());
                if (diag != null) throw new DiagnosticException(diag);
            }
        }
        TypedExpression resolved = resolveExpressionTyped(exprTokens, ctx, env);
        if (resolved == null) {
            return false;
        }
        LumenType varType = ref.type();
        int colStart = exprTokens.get(0).start();
        int colEnd = exprTokens.get(exprTokens.size() - 1).end();
        LumenDiagnostic diag = TypeChecker.checkAssignment(varType, resolved.type, name, ctx.line(), ctx.raw(), colStart, colEnd);
        if (diag != null) throw new DiagnosticException(diag);
        ctx.out().line(ref.java() + " = " + resolved.java + ";");
        if (varType instanceof NullableType) {
            env.markNullState(name, isNone ? TypeEnv.NullState.NULL : TypeEnv.NullState.NON_NULL, ctx.line(), ctx.raw());
        }
        if (env.isStored(name)) {
            ctx.out().line(env.storedClassName(name) + ".set(" + env.getStoredKey(name) + ", " + ref.java() + ");");
        }
        return true;
    }

    private static void emitDeclaration(@NotNull String name, @NotNull List<Token> exprTokens, @NotNull List<Token> pipelineTokens, @NotNull EmitContextImpl ctx, @NotNull TypeEnv env) {
        String nameError = VarNameValidator.validate(name);
        if (nameError != null) {
            throw new DiagnosticException(LumenDiagnostic.error("E502", nameError)
                    .at(ctx.line(), ctx.raw())
                    .highlight(pipelineTokens.get(1).start(), pipelineTokens.get(1).end())
                    .label("invalid variable name")
                    .build());
        }

        if (env.blockContext().isRoot()) {
            throw new DiagnosticException(LumenDiagnostic.error("E502", "'set' cannot be used at the top level of a script")
                    .at(ctx.line(), ctx.raw())
                    .highlight(pipelineTokens.get(0).start(), pipelineTokens.get(0).end())
                    .label("top-level 'set' not allowed")
                    .help("use 'global " + name + " with default <value>' instead")
                    .build());
        }

        if (exprTokens.size() >= 2 && exprTokens.get(0).text().equalsIgnoreCase("nullable")) {
            emitNullableDeclaration(name, exprTokens, pipelineTokens, ctx, env);
            return;
        }

        Expr e = ExprParser.parse(exprTokens, env, ctx.line(), ctx.raw());

        String java;
        VarRef inheritedRef = null;
        LumenType exprLumenType = e.resolvedType();
        Map<String, Object> resolvedMetadata = Map.of();

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
                Token varToken = exprTokens.get(0);
                String suggestion = FuzzyMatch.closest(r.name(), env.allVisibleVarNames());
                    LumenDiagnostic.Builder diagBuilder = LumenDiagnostic.error("E500", "Variable '" + r.name() + "' not found")
                            .at(ctx.line(), ctx.raw())
                            .highlight(varToken.start(), varToken.end())
                            .label("undefined variable");
                if (suggestion != null) diagBuilder.help("did you mean '" + suggestion + "'?");
                else diagBuilder.help("make sure the variable is defined before using it");
                LumenDiagnostic diag = diagBuilder.build();
                throw new DiagnosticException(diag);
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
                    exprLumenType = exprResult.type();
                    resolvedMetadata = exprResult.metadata();
                } else {
                    ExpressionResult resolvedResult = ExprResolver.resolveWithType(raw.tokens(), ctx.codegenContext(), env);
                    if (resolvedResult != null) {
                        java = resolvedResult.java();
                        exprLumenType = resolvedResult.type();
                        resolvedMetadata = resolvedResult.metadata();
                    } else {
                        throw new DiagnosticException(buildExpressionDiagnostic(raw.tokens(), ctx.line(), ctx.raw(), env));
                    }
                }
            } else {
                Token singleToken = raw.tokens().get(0);
                if (singleToken.kind() == TokenKind.IDENT) {
                    if (isNullKeyword(singleToken.text())) {
                        Token noneToken = raw.tokens().get(0);
                        LumenDiagnostic diag = LumenDiagnostic.error("E101", "Cannot declare variable with 'none' without a type")
                                .at(ctx.line(), ctx.raw())
                                .highlight(noneToken.start(), noneToken.end())
                                .label("'none' has no type")
                                .help("use 'set " + name + " to nullable <type>' to declare a nullable variable")
                                .build();
                        throw new DiagnosticException(diag);
                    } else if (env.lookupVar(singleToken.text()) == null) {
                        String suggestion = FuzzyMatch.closest(singleToken.text(), env.allVisibleVarNames());
                        LumenDiagnostic.Builder diagBuilder = LumenDiagnostic.error("E500", "Variable '" + singleToken.text() + "' not found")
                                .at(ctx.line(), ctx.raw())
                                .highlight(singleToken.start(), singleToken.end())
                                .label("undefined variable");
                        if (suggestion != null) diagBuilder.help("did you mean '" + suggestion + "'?");
                        else diagBuilder.help("make sure the variable is defined before using it");
                        throw new DiagnosticException(diagBuilder.build());
                    } else {
                        java = ExprResolver.joinTokens(raw.tokens());
                    }
                } else {
                    java = ExprResolver.joinTokens(raw.tokens());
                }
            }
        }

        LumenType resolvedLumenType;
        if (inheritedRef != null) {
            resolvedLumenType = inheritedRef.type();
        } else {
            resolvedLumenType = exprLumenType;
        }
        String typeDecl = resolvedLumenType.javaTypeName();
        ctx.out().line(typeDecl + " " + name + " = " + java + ";");
        VarRef varRef = new VarRef(resolvedLumenType, name, resolvedMetadata);
        env.defineVar(name, varRef);
        if (env.blockContext().parent() != null) {
            env.blockContext().parent().defineVar(name, varRef);
        }
    }

    private static void emitNullableDeclaration(@NotNull String name, @NotNull List<Token> exprTokens, @NotNull List<Token> pipelineTokens, @NotNull EmitContextImpl ctx, @NotNull TypeEnv env) {
        String typeName = exprTokens.get(1).text();
        LumenType innerType = LumenType.fromName(typeName);
        if (innerType == null) {
            Token typeToken = exprTokens.get(1);
            String suggestion = FuzzyMatch.closest(typeName, LumenType.allKnownTypeNames());
            LumenDiagnostic.Builder diagBuilder = LumenDiagnostic.error("E501", "Unknown type '" + typeName + "'")
                    .at(ctx.line(), ctx.raw())
                    .highlight(typeToken.start(), typeToken.end())
                    .label("not a recognized type");
            if (suggestion != null) diagBuilder.help("did you mean '" + suggestion + "'?");
            else diagBuilder.help("expected a type name like 'string', 'int', 'player', etc.");
            LumenDiagnostic diag = diagBuilder.build();
            throw new DiagnosticException(diag);
        }
        NullableType nullableType = innerType.wrapAsNullable();
        String java;
        if (exprTokens.size() == 2) {
            java = "null";
        } else {
            List<Token> valueTokens = exprTokens.subList(2, exprTokens.size());
            java = resolveExpressionJava(valueTokens, ctx, env);
            if (java == null) {
                throw new DiagnosticException(buildExpressionDiagnostic(valueTokens, ctx.line(), ctx.raw(), env));
            }
        }
        ctx.out().line(nullableType.javaTypeName() + " " + name + " = " + java + ";");
        VarRef varRef = new VarRef(nullableType, name);
        env.defineVar(name, varRef);
        env.recordNullableVarInfo(name, new TypeEnv.NullableVarInfo(ctx.line(), ctx.raw()));
        env.markNullState(name, exprTokens.size() == 2 ? TypeEnv.NullState.NULL : TypeEnv.NullState.NON_NULL, ctx.line(), ctx.raw());
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
        TypedExpression typed = resolveExpressionTyped(tokens, ctx, env);
        return typed != null ? typed.java : null;
    }

    private static @Nullable TypedExpression resolveExpressionTyped(@NotNull List<Token> tokens, @NotNull EmitContextImpl ctx, @NotNull TypeEnv env) {
        Expr e = ExprParser.parse(tokens, env, ctx.line(), ctx.raw());
        if (e instanceof Expr.Literal l) {
            String java;
            if (l.value() instanceof String s) {
                java = PlaceholderExpander.expand(s, env);
            } else if (l.value() instanceof Boolean b) {
                java = b.toString();
            } else {
                java = l.value().toString();
            }
            return new TypedExpression(java, l.resolvedType());
        }
        if (e instanceof Expr.RefExpr r) {
            VarRef varRef = env.lookupVar(r.name());
            return varRef != null ? new TypedExpression(varRef.java(), varRef.type()) : null;
        }
        if (e instanceof Expr.MathExpr m) return new TypedExpression(m.java(), m.resolvedType());
        Expr.RawExpr raw = (Expr.RawExpr) e;
        if (raw.tokens().size() > 1) {
            ExpressionResult result = tryExpressionPattern(raw.tokens(), ctx, env);
            if (result != null) return new TypedExpression(result.java(), result.type());
            ExpressionResult resolved = ExprResolver.resolveWithType(raw.tokens(), ctx.codegenContext(), env);
            if (resolved != null) return new TypedExpression(resolved.java(), resolved.type());
            return null;
        }
        Token single = raw.tokens().get(0);
        if (single.kind() == TokenKind.IDENT) {
            if (isNullKeyword(single.text())) return new TypedExpression("null", PrimitiveType.STRING);
            VarRef varRef = env.lookupVar(single.text());
            return varRef != null ? new TypedExpression(varRef.java(), varRef.type()) : null;
        }
        return new TypedExpression(ExprResolver.joinTokens(raw.tokens()), PrimitiveType.STRING);
    }

    private record TypedExpression(@NotNull String java, @NotNull LumenType type) {
    }

    private static @NotNull LumenDiagnostic buildExpressionDiagnostic(@NotNull List<Token> tokens, int line, @NotNull String raw, @NotNull TypeEnv env) {
        List<PatternSimulator.Suggestion> suggestions = PatternSimulator.suggestExpressions(tokens, PatternRegistry.instance(), env);
        if (!suggestions.isEmpty()) {
            return SuggestionDiagnostics.build("E502", "Cannot resolve expression", line, raw, tokens, suggestions.get(0));
        }
        return SuggestionDiagnostics.buildNoSuggestion("E502", "Cannot resolve expression", line, raw, tokens);
    }
}
