package dev.lumenlang.lumen.plugin.defaults.emit.form;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.emit.BlockFormHandler;
import dev.lumenlang.lumen.api.emit.EmitContext;
import dev.lumenlang.lumen.api.emit.ScriptLine;
import dev.lumenlang.lumen.api.emit.ScriptToken;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.type.CollectionType;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.NullableType;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.pipeline.codegen.BindingContext;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.language.emit.EmitContextImpl;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredExpressionMatch;
import dev.lumenlang.lumen.pipeline.language.resolve.PatternSimulator;
import dev.lumenlang.lumen.pipeline.language.resolve.SuggestionDiagnostics;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.language.typed.Expr;
import dev.lumenlang.lumen.pipeline.language.typed.ExprParser;
import dev.lumenlang.lumen.pipeline.language.validator.VarNameValidator;
import dev.lumenlang.lumen.pipeline.placeholder.PlaceholderExpander;
import dev.lumenlang.lumen.pipeline.type.TypeAnnotationParser;
import dev.lumenlang.lumen.pipeline.util.FuzzyMatch;
import dev.lumenlang.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Block form handler for the {@code global:} block.
 *
 * <p>Parses typed global variable declarations from the block's children and registers them
 * in the type environment. Each child line follows the syntax:
 * <pre>
 * [stored] [scoped to &lt;type&gt;[s]] &lt;name&gt;: &lt;type&gt; [with default &lt;expr&gt;]
 * </pre>
 *
 * <p>Collections auto-default to empty instances. Nullable types auto-default to {@code null}.
 * Non-nullable, non-collection types without a default produce a parse error.
 *
 * <p>Exactly one {@code global:} block is allowed per script.
 */
@Registration(order = -2000)
@SuppressWarnings("unused")
public final class GlobalBlock implements BlockFormHandler {

    private static final String GLOBAL_BLOCK_KEY = "__global_block_declared";

    @Call
    public void register(@NotNull LumenAPI api) {
        api.emitters().blockForm(this);
    }

    @Override
    public boolean matches(@NotNull List<? extends ScriptToken> headTokens) {
        return headTokens.size() == 1
                && headTokens.get(0).tokenType() == ScriptToken.TokenType.IDENT
                && headTokens.get(0).text().equalsIgnoreCase("global");
    }

    @Override
    public void handle(@NotNull List<? extends ScriptToken> headTokens, @NotNull List<? extends ScriptLine> children, @NotNull EmitContext ctx) {
        TypeEnv env = (TypeEnv) ctx.env();

        if (env.has(GLOBAL_BLOCK_KEY)) {
            throw new DiagnosticException(LumenDiagnostic.error("E600", "Duplicate global block")
                    .at(ctx.line(), ctx.raw())
                    .label("only one 'global:' block is allowed per script")
                    .help("merge all global declarations into a single 'global:' block")
                    .build());
        }
        env.put(GLOBAL_BLOCK_KEY, true);

        for (ScriptLine child : children) {
            List<Token> tokens = EmitContextImpl.toPipelineTokens(child.tokens());
            if (tokens.isEmpty()) continue;
            parseGlobalLine(tokens, child.lineNumber(), child.raw(), env, ctx);
        }
    }

    private void parseGlobalLine(@NotNull List<Token> tokens, int line, @NotNull String raw, @NotNull TypeEnv env, @NotNull EmitContext ctx) {
        int idx = 0;

        boolean stored = false;
        if (idx < tokens.size() && tokens.get(idx).text().equalsIgnoreCase("stored")) {
            stored = true;
            idx++;
        }

        boolean scoped = false;
        LumenType scopeType = null;
        if (idx < tokens.size() && tokens.get(idx).text().equalsIgnoreCase("scoped")) {
            idx++;
            if (idx >= tokens.size() || !tokens.get(idx).text().equalsIgnoreCase("to")) {
                throw new DiagnosticException(LumenDiagnostic.error("E601", "Expected 'to' after 'scoped'")
                        .at(line, raw)
                        .highlight(tokens.get(idx - 1).start(), tokens.get(idx - 1).end())
                        .label("expected 'scoped to <type>'")
                        .help("example: scoped to player")
                        .build());
            }
            idx++;
            if (idx >= tokens.size()) {
                throw new DiagnosticException(LumenDiagnostic.error("E601", "Expected scope type after 'scoped to'")
                        .at(line, raw)
                        .label("missing scope type")
                        .help("example: scoped to player")
                        .build());
            }
            String scopeTypeName = tokens.get(idx).text().toLowerCase();
            if (scopeTypeName.endsWith("s") && scopeTypeName.length() > 1) {
                scopeTypeName = scopeTypeName.substring(0, scopeTypeName.length() - 1);
            }
            scopeType = LumenType.fromName(scopeTypeName);
            if (scopeType == null) {
                String closest = FuzzyMatch.closest(scopeTypeName, LumenType.allKnownTypeNames());
                LumenDiagnostic.Builder diag = LumenDiagnostic.error("E602", "Unknown scope type '" + tokens.get(idx).text() + "'")
                        .at(line, raw)
                        .highlight(tokens.get(idx).start(), tokens.get(idx).end());
                if (closest != null) {
                    diag.label("did you mean '" + closest + "'?");
                } else {
                    diag.label("not a recognized type");
                }
                diag.help("known types: player, entity, etc.");
                throw new DiagnosticException(diag.build());
            }
            scoped = true;
            idx++;
        }

        if (idx >= tokens.size()) {
            throw new DiagnosticException(LumenDiagnostic.error("E603", "Expected variable name")
                    .at(line, raw)
                    .label("missing variable name")
                    .help("syntax: [stored] [scoped to <type>] <name>: <type> [with default <expr>]")
                    .build());
        }

        String name = tokens.get(idx).text();
        Token nameToken = tokens.get(idx);
        idx++;

        String nameError = VarNameValidator.validate(name);
        if (nameError != null) {
            throw new DiagnosticException(LumenDiagnostic.error("E604", "Invalid variable name")
                    .at(line, raw)
                    .highlight(nameToken.start(), nameToken.end())
                    .label(nameError)
                    .build());
        }

        if (env.isGlobal(name)) {
            throw new DiagnosticException(LumenDiagnostic.error("E605", "Duplicate global variable '" + name + "'")
                    .at(line, raw)
                    .highlight(nameToken.start(), nameToken.end())
                    .label("'" + name + "' is already declared as a global")
                    .build());
        }

        if (idx >= tokens.size() || !tokens.get(idx).text().equals(":")) {
            throw new DiagnosticException(LumenDiagnostic.error("E606", "Expected ':' after variable name")
                    .at(line, raw)
                    .highlight(nameToken.start(), nameToken.end())
                    .label("expected '" + name + ": <type>'")
                    .help("syntax: " + name + ": string")
                    .build());
        }
        idx++;

        if (idx >= tokens.size()) {
            throw new DiagnosticException(LumenDiagnostic.error("E607", "Expected type after ':'")
                    .at(line, raw)
                    .label("missing type annotation")
                    .help("syntax: " + name + ": <type>   (e.g. string, int, list of string, nullable player)")
                    .build());
        }

        TypeAnnotationParser.ParseResult typeParseResult = TypeAnnotationParser.parseDetailed(tokens, idx, env::lookupDataSchema);
        if (typeParseResult instanceof TypeAnnotationParser.ParseResult.Failure f) {
            TypeAnnotationParser.ParseError error = f.error();
            int errorIdx = Math.min(error.tokenOffset(), tokens.size() - 1);
            Token errorToken = tokens.get(errorIdx);
            LumenDiagnostic.Builder diag = LumenDiagnostic.error("E608", "Invalid type annotation")
                    .at(line, raw)
                    .highlight(errorToken.start(), errorToken.end());
            if (error.suggestion() != null) {
                diag.label(error.message() + ", did you mean '" + error.suggestion() + "'?");
            } else {
                diag.label(error.message());
            }
            diag.help("known types: int, string, boolean, player, list of <type>, map of <type> to <type>, nullable <type>, etc.");
            throw new DiagnosticException(diag.build());
        }
        TypeAnnotationParser typeResult = ((TypeAnnotationParser.ParseResult.Success) typeParseResult).parser();

        LumenType declaredType = typeResult.type();
        idx += typeResult.tokensConsumed();

        List<Token> exprTokens = null;
        if (idx < tokens.size() && tokens.get(idx).text().equalsIgnoreCase("with")) {
            idx++;
            if (idx >= tokens.size() || !tokens.get(idx).text().equalsIgnoreCase("default")) {
                throw new DiagnosticException(LumenDiagnostic.error("E609", "Expected 'default' after 'with'")
                        .at(line, raw)
                        .label("expected 'with default <expr>'")
                        .build());
            }
            idx++;
            if (idx >= tokens.size()) {
                throw new DiagnosticException(LumenDiagnostic.error("E609", "Expected expression after 'with default'")
                        .at(line, raw)
                        .label("missing default value expression")
                        .build());
            }
            exprTokens = tokens.subList(idx, tokens.size());
        } else if (idx < tokens.size()) {
            throw new DiagnosticException(LumenDiagnostic.error("E610", "Unexpected token '" + tokens.get(idx).text() + "'")
                    .at(line, raw)
                    .highlight(tokens.get(idx).start(), tokens.get(idx).end())
                    .label("expected 'with default <expr>' or end of line")
                    .build());
        }

        String defaultJava = resolveDefault(declaredType, exprTokens, name, line, raw, env, ctx);
        String className = ctx.codegen().className();
        Map<String, Object> exprMetadata = resolveExprMetadata(exprTokens, env, ctx);

        env.registerGlobal(new TypeEnv.GlobalVarInfo(name, defaultJava, className, scoped, stored, exprMetadata, declaredType, scopeType));
    }

    private @NotNull String resolveDefault(@NotNull LumenType declaredType, @Nullable List<Token> exprTokens, @NotNull String name, int line, @NotNull String raw, @NotNull TypeEnv env, @NotNull EmitContext ctx) {
        if (exprTokens != null) {
            return resolveExprJava(exprTokens, line, raw, env, ctx);
        }

        if (declaredType instanceof CollectionType ct) {
            if (ct.id().equals("LIST") && !ct.typeArguments().isEmpty()) {
                ctx.codegen().addImport("java.util.ArrayList");
                return "new ArrayList<" + boxedJavaType(ct.typeArguments().get(0)) + ">()";
            }
            if (ct.id().equals("MAP") && ct.typeArguments().size() == 2) {
                ctx.codegen().addImport("java.util.LinkedHashMap");
                return "new LinkedHashMap<" + boxedJavaType(ct.typeArguments().get(0)) + ", " + boxedJavaType(ct.typeArguments().get(1)) + ">()";
            }
        }
        if (declaredType instanceof NullableType) return "null";

        throw new DiagnosticException(LumenDiagnostic.error("E611", "Non-nullable type '" + declaredType.displayName() + "' requires a default value")
                .at(line, raw)
                .label("'" + name + "' has no default and is not nullable")
                .help("add 'with default <value>', or change the type to 'nullable " + declaredType.displayName() + "'")
                .build());
    }

    private @NotNull String resolveExprJava(@NotNull List<Token> exprTokens, int line, @NotNull String raw, @NotNull TypeEnv env, @NotNull EmitContext ctx) {
        Expr expr = ExprParser.parse(exprTokens, env);
        if (!(expr instanceof Expr.RawExpr)) return resolveSimpleExprJava(expr, env);

        RegisteredExpressionMatch exprMatch = PatternRegistry.instance().matchExpression(exprTokens, env);
        if (exprMatch != null) {
            BindingContext bc = new BindingContext(exprMatch.match(), env, ((EmitContextImpl) ctx).codegenContext(), env.blockContext());
            ExpressionResult result = exprMatch.reg().handler().handle(bc);
            return result.java();
        }

        List<Token> rawTokens = ((Expr.RawExpr) expr).tokens();
        List<PatternSimulator.Suggestion> suggestions = PatternSimulator.suggestExpressions(rawTokens, PatternRegistry.instance(), env);
        if (!suggestions.isEmpty()) {
            throw new DiagnosticException(SuggestionDiagnostics.build("E502", "Cannot resolve default expression", line, raw, rawTokens, suggestions.get(0)));
        }
        throw new DiagnosticException(SuggestionDiagnostics.buildNoSuggestion("E502", "Cannot resolve default expression", line, raw, rawTokens));
    }

    private @Nullable Map<String, Object> resolveExprMetadata(@Nullable List<Token> exprTokens, @NotNull TypeEnv env, @NotNull EmitContext ctx) {
        if (exprTokens == null) return null;
        Expr expr = ExprParser.parse(exprTokens, env);
        if (!(expr instanceof Expr.RawExpr)) return null;
        RegisteredExpressionMatch exprMatch = PatternRegistry.instance().matchExpression(exprTokens, env);
        if (exprMatch == null) return null;
        BindingContext bc = new BindingContext(exprMatch.match(), env, ((EmitContextImpl) ctx).codegenContext(), env.blockContext());
        ExpressionResult result = exprMatch.reg().handler().handle(bc);
        return result.metadata();
    }

    private @NotNull String resolveSimpleExprJava(@NotNull Expr expr, @NotNull TypeEnv env) {
        if (expr instanceof Expr.Literal l) {
            if (l.value() == null) return "null";
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

    private static @NotNull String boxedJavaType(@NotNull LumenType type) {
        if (type instanceof PrimitiveType p) return p.boxedName();
        return type.javaTypeName();
    }
}
