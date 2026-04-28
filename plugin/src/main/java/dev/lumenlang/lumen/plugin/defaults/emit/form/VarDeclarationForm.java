package dev.lumenlang.lumen.plugin.defaults.emit.form;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.CollectionType;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.NullableType;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.api.util.FuzzyMatch;
import dev.lumenlang.lumen.pipeline.codegen.BlockContext;
import dev.lumenlang.lumen.pipeline.codegen.HandlerContextImpl;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
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
import dev.lumenlang.lumen.pipeline.type.TypeAnnotationParser;
import dev.lumenlang.lumen.pipeline.type.TypeChecker;
import dev.lumenlang.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Registers the {@code set <name> to <expr>} statement pattern for local variable
 * declaration, reassignment, and scoped global operations.
 */
@Registration(order = -1998)
@SuppressWarnings({"unused", "DataFlowIssue"})
public final class VarDeclarationForm {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().statement(b -> b
                .by("Lumen")
                .pattern("set %name:IDENT% to %val:EXPR%")
                .description("Declares or reassigns a variable.")
                .example("set x to 5")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(VarDeclarationForm::handle));
    }

    private static void handle(@NotNull HandlerContext ctx) {
        HandlerContextImpl emitCtx = (HandlerContextImpl) ctx;
        TypeEnv env = (TypeEnv) ctx.env();

        Token nameToken = emitCtx.bound("name").tokens().get(0);
        String name = nameToken.text();
        List<Token> exprTokens = emitCtx.bound("val").tokens();

        EnvironmentAccess.GlobalInfo globalInfo = env.getGlobalInfo(name);
        if (globalInfo != null && !env.isGlobalField(name)) {
            emitScopedGlobalSet(name, globalInfo, exprTokens, emitCtx, env);
            return;
        }

        VarRef existing = env.lookupVar(name);
        if (existing != null) {
            if (tryReassignment(name, existing, exprTokens, nameToken, emitCtx, env)) {
                return;
            }
            if (globalInfo != null) {
                emitScopedGlobalSet(name, globalInfo, exprTokens, emitCtx, env);
                return;
            }
            throw new DiagnosticException(LumenDiagnostic.error("Cannot resolve expression")
                    .at(emitCtx.line(), emitCtx.raw())
                    .highlight(exprTokens.get(0).start(), exprTokens.get(exprTokens.size() - 1).end())
                    .label("'" + ExprResolver.joinTokens(exprTokens) + "' is not a recognized expression")
                    .help("check spelling or ensure the variable or expression is defined")
                    .build());
        }

        emitDeclaration(name, exprTokens, nameToken, emitCtx, env);
    }

    private static void emitScopedGlobalSet(@NotNull String name, @NotNull EnvironmentAccess.GlobalInfo info, @NotNull List<Token> exprTokens, @NotNull HandlerContextImpl ctx, @NotNull TypeEnv env) {
        if (!info.scoped()) {
            throw new DiagnosticException(LumenDiagnostic.error("Variable '" + name + "' is not a scoped global")
                    .at(ctx.line(), ctx.raw())
                    .highlight(exprTokens.get(0).start(), exprTokens.get(exprTokens.size() - 1).end())
                    .label("'" + name + "' is not scoped")
                    .help("declare it inside a 'global:' block with 'scoped to <type> " + name + ": <type>' for per-entity access")
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
            throw new DiagnosticException(LumenDiagnostic.error("Scoped global '" + name + "' requires a scope")
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
            throw new DiagnosticException(LumenDiagnostic.error("Invalid scope expression")
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
            LumenDiagnostic.Builder diagBuilder = LumenDiagnostic.error("Variable '" + scopeVarName + "' not found")
                    .at(ctx.line(), ctx.raw())
                    .highlight(scopeTokens.get(0).start(), scopeTokens.get(0).end())
                    .label("undefined variable");
            if (suggestion != null) diagBuilder.help("did you mean '" + suggestion + "'?");
            else diagBuilder.help("make sure the variable is defined before using it");
            throw new DiagnosticException(diagBuilder.build());
        }
        if (scopeRef.objectType() == null) {
            throw new DiagnosticException(LumenDiagnostic.error("Scope variable '" + scopeVarName + "' has no type")
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
        if (fieldRef != null && !isScopedGlobal(fieldRef)) {
            ctx.out().line(fieldRef.java() + " = " + valueJava + ";");
        }
    }

    private static boolean isScopedGlobal(@NotNull VarRef ref) {
        return ref.globalInfo() != null && ref.globalInfo().scoped();
    }

    private static boolean tryReassignment(@NotNull String name, @NotNull VarRef ref, @NotNull List<Token> exprTokens, @NotNull Token nameToken, @NotNull HandlerContextImpl ctx, @NotNull TypeEnv env) {
        BlockContext block = env.blockContext();
        if (block.getEnvFromParents("__lambda_block") != null && env.isVarCapturedByLambda(name)) {
            throw new DiagnosticException(LumenDiagnostic.error("Cannot modify '" + name + "' inside a schedule block")
                    .at(ctx.line(), ctx.raw())
                    .highlight(nameToken.start(), nameToken.end())
                    .label("captured variable cannot be modified")
                    .help("declare it inside a 'global:' block as '" + name + ": <type> with default <value>'")
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
            ctx.out().line(ref.java() + " = null;");
            env.markNullState(name, TypeEnv.NullState.NULL, ctx.line(), ctx.raw());
            if (env.isStored(name)) {
                ctx.out().line(env.storedClassName(name) + ".set(" + env.getStoredKey(name) + ", " + ref.java() + ");");
            }
            return true;
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
        env.recordDeclaration(name, ctx.line(), ctx.raw());
        if (varType instanceof NullableType) {
            if (resolved.type instanceof NullableType) {
                env.clearNonNull(name);
            } else {
                env.markNullState(name, TypeEnv.NullState.NON_NULL, ctx.line(), ctx.raw());
            }
        }
        if (env.isStored(name)) {
            ctx.out().line(env.storedClassName(name) + ".set(" + env.getStoredKey(name) + ", " + ref.java() + ");");
        }
        return true;
    }

    private static void emitDeclaration(@NotNull String name, @NotNull List<Token> exprTokens, @NotNull Token nameToken, @NotNull HandlerContextImpl ctx, @NotNull TypeEnv env) {
        String nameError = VarNameValidator.validate(name);
        if (nameError != null) {
            throw new DiagnosticException(LumenDiagnostic.error(nameError)
                    .at(ctx.line(), ctx.raw())
                    .highlight(nameToken.start(), nameToken.end())
                    .label("invalid variable name")
                    .build());
        }

        if (exprTokens.size() >= 2 && exprTokens.get(0).text().equalsIgnoreCase("nullable")) {
            emitNullableDeclaration(name, exprTokens, nameToken, ctx, env);
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
                LumenDiagnostic.Builder diagBuilder = LumenDiagnostic.error("Variable '" + r.name() + "' not found")
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
                    ExpressionResult resolvedResult = ExprResolver.resolveWithTypeNoDirectMatch(raw.tokens(), ctx.codegenContext(), env);
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
                        LumenDiagnostic diag = LumenDiagnostic.error("Cannot declare variable with 'none' without a type")
                                .at(ctx.line(), ctx.raw())
                                .highlight(noneToken.start(), noneToken.end())
                                .label("'none' has no type")
                                .help("use 'set " + name + " to nullable <type>' to declare a nullable variable")
                                .build();
                        throw new DiagnosticException(diag);
                    } else if (env.lookupVar(singleToken.text()) == null) {
                        String suggestion = FuzzyMatch.closest(singleToken.text(), env.allVisibleVarNames());
                        LumenDiagnostic.Builder diagBuilder = LumenDiagnostic.error("Variable '" + singleToken.text() + "' not found")
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
        VarRef varRef = new VarRef(name, resolvedLumenType, name, resolvedMetadata);
        env.defineVar(name, varRef);
        env.recordDeclaration(name, ctx.line(), ctx.raw());
        if (resolvedLumenType instanceof NullableType) {
            env.recordNullableVarInfo(name, new TypeEnv.NullableVarInfo(ctx.line(), ctx.raw()));
            if (!(exprLumenType instanceof NullableType)) {
                env.markNullState(name, TypeEnv.NullState.NON_NULL, ctx.line(), ctx.raw());
            }
        }
        if (env.blockContext().parent() != null) {
            env.blockContext().parent().defineVar(name, varRef);
        }
    }

    private static void emitNullableDeclaration(@NotNull String name, @NotNull List<Token> exprTokens, @NotNull Token nameToken, @NotNull HandlerContextImpl ctx, @NotNull TypeEnv env) {
        TypeAnnotationParser.ParseResult result = TypeAnnotationParser.parseDetailed(exprTokens, 0, env::lookupDataSchema);
        if (result instanceof TypeAnnotationParser.ParseResult.Failure f) {
            throw new DiagnosticException(SuggestionDiagnostics.buildTypeFailure("Invalid nullable type", ctx.line(), ctx.raw(), exprTokens, f));
        }
        TypeAnnotationParser parsed = ((TypeAnnotationParser.ParseResult.Success) result).parser();
        NullableType nullableType = (NullableType) parsed.type();
        int consumed = parsed.tokensConsumed();
        List<Token> valueTokens = consumed < exprTokens.size() ? exprTokens.subList(consumed, exprTokens.size()) : null;
        boolean isNone = valueTokens != null && valueTokens.size() == 1 && isNullKeyword(valueTokens.get(0).text());
        String java;
        TypeEnv.NullState nullState;
        if (valueTokens == null || isNone) {
            java = resolveNullableDefault(nullableType, isNone, ctx);
            nullState = isNone || java.equals("null") ? TypeEnv.NullState.NULL : TypeEnv.NullState.NON_NULL;
        } else {
            java = resolveExpressionJava(valueTokens, ctx, env);
            if (java == null) throw new DiagnosticException(buildExpressionDiagnostic(valueTokens, ctx.line(), ctx.raw(), env));
            nullState = TypeEnv.NullState.NON_NULL;
        }
        ctx.out().line(nullableType.javaTypeName() + " " + name + " = " + java + ";");
        Map<String, Object> metadata = parsed.dataSchemaName() != null ? Map.of("data_type", parsed.dataSchemaName()) : Map.of();
        VarRef varRef = new VarRef(name, nullableType, name, metadata);
        env.defineVar(name, varRef);
        env.recordDeclaration(name, ctx.line(), ctx.raw());
        env.recordNullableVarInfo(name, new TypeEnv.NullableVarInfo(ctx.line(), ctx.raw()));
        env.markNullState(name, nullState, ctx.line(), ctx.raw());
        if (env.blockContext().parent() != null) {
            env.blockContext().parent().defineVar(name, varRef);
        }
    }

    private static @NotNull String resolveNullableDefault(@NotNull NullableType type, boolean explicitNone, @NotNull HandlerContextImpl ctx) {
        if (explicitNone) return "null";
        LumenType inner = type.inner();
        if (inner instanceof CollectionType ct) {
            if (ct.id().equals("LIST") && !ct.typeArguments().isEmpty()) {
                ctx.codegen().addImport("java.util.ArrayList");
                return "new ArrayList<" + boxedJavaType(ct.typeArguments().get(0)) + ">()";
            }
            if (ct.id().equals("MAP") && ct.typeArguments().size() == 2) {
                ctx.codegen().addImport("java.util.LinkedHashMap");
                return "new LinkedHashMap<" + boxedJavaType(ct.typeArguments().get(0)) + ", " + boxedJavaType(ct.typeArguments().get(1)) + ">()";
            }
        }
        return "null";
    }

    private static @NotNull String boxedJavaType(@NotNull LumenType type) {
        if (type instanceof PrimitiveType p) return p.boxedName();
        return type.javaTypeName();
    }

    private static boolean isNullKeyword(@NotNull String text) {
        return text.equalsIgnoreCase("none") || text.equalsIgnoreCase("null");
    }

    private static @Nullable ExpressionResult tryExpressionPattern(@NotNull List<Token> tokens, @NotNull HandlerContextImpl ctx, @NotNull TypeEnv env) {
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
            HandlerContextImpl hctx = new HandlerContextImpl(match.match(), env, ctx.codegenContext(), block, null, ctx.line(), ctx.raw());
            return match.reg().handler().handle(hctx);
        } catch (DiagnosticException e) {
            throw e;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static @Nullable String resolveExpressionJava(@NotNull List<Token> tokens, @NotNull HandlerContextImpl ctx, @NotNull TypeEnv env) {
        TypedExpression typed = resolveExpressionTyped(tokens, ctx, env);
        return typed != null ? typed.java : null;
    }

    private static @Nullable TypedExpression resolveExpressionTyped(@NotNull List<Token> tokens, @NotNull HandlerContextImpl ctx, @NotNull TypeEnv env) {
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
            if (varRef == null) return null;
            LumenType type = varRef.type();
            if (type instanceof NullableType nt && env.nullState(varRef.java()) == TypeEnv.NullState.NON_NULL) {
                type = nt.inner();
            }
            return new TypedExpression(varRef.java(), type);
        }
        if (e instanceof Expr.MathExpr m) return new TypedExpression(m.java(), m.resolvedType());
        Expr.RawExpr raw = (Expr.RawExpr) e;
        if (raw.tokens().size() > 1) {
            ExpressionResult result = tryExpressionPattern(raw.tokens(), ctx, env);
            if (result != null) return new TypedExpression(result.java(), result.type());
            ExpressionResult resolved = ExprResolver.resolveWithTypeNoDirectMatch(raw.tokens(), ctx.codegenContext(), env);
            if (resolved != null) return new TypedExpression(resolved.java(), resolved.type());
            return null;
        }
        Token single = raw.tokens().get(0);
        if (single.kind() == TokenKind.IDENT) {
            if (isNullKeyword(single.text())) return new TypedExpression("null", PrimitiveType.STRING);
            VarRef varRef = env.lookupVar(single.text());
            if (varRef == null) return null;
            LumenType type = varRef.type();
            if (type instanceof NullableType nt && env.nullState(varRef.java()) == TypeEnv.NullState.NON_NULL) {
                type = nt.inner();
            }
            return new TypedExpression(varRef.java(), type);
        }
        return new TypedExpression(ExprResolver.joinTokens(raw.tokens()), PrimitiveType.STRING);
    }

    private record TypedExpression(@NotNull String java, @NotNull LumenType type) {
    }

    private static @NotNull LumenDiagnostic buildExpressionDiagnostic(@NotNull List<Token> tokens, int line, @NotNull String raw, @NotNull TypeEnv env) {
        List<PatternSimulator.Suggestion> suggestions = PatternSimulator.suggestExpressions(tokens, PatternRegistry.instance(), env);
        if (!suggestions.isEmpty()) {
            return SuggestionDiagnostics.build("Cannot resolve expression", line, raw, tokens, suggestions, env);
        }
        return SuggestionDiagnostics.buildNoSuggestion("Cannot resolve expression", line, raw, tokens, env);
    }
}
