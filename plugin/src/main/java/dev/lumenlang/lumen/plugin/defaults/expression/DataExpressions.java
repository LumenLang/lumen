package dev.lumenlang.lumen.plugin.defaults.expression;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.codegen.TypeEnv;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.emit.ScriptToken;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.BuiltinLumenTypes;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.api.util.FuzzyMatch;
import dev.lumenlang.lumen.pipeline.codegen.HandlerContextImpl;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.data.DataSchema;
import dev.lumenlang.lumen.pipeline.java.compiled.DataInstance;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredExpressionMatch;
import dev.lumenlang.lumen.pipeline.language.resolve.ExprResolver;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator;
import dev.lumenlang.lumen.pipeline.language.simulator.suggestions.SuggestionDiagnostics;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.language.tokenization.TokenKind;
import dev.lumenlang.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Registers data class expression patterns for construction and field access.
 */
@Registration
@SuppressWarnings("unused")
public final class DataExpressions {

    /**
     * Produces a typed field access result by looking up the field's {@link LumenType}
     * from the compile-time {@link DataSchema} and generating a properly cast Java expression.
     *
     * @param ctx    the binding access for the current pattern match
     * @param rawGet the Java expression for the raw {@code DataInstance.get()} call
     * @return an expression result with the correctly typed Java expression
     */
    private static @NotNull ExpressionResult typedFieldResult(@NotNull HandlerContext ctx, @NotNull String rawGet) {
        LumenType fieldType = resolveFieldLumenType(ctx);
        if (fieldType != null) {
            return new ExpressionResult(DataSchema.castFromObject(fieldType, rawGet), fieldType);
        }
        return new ExpressionResult(rawGet, BuiltinLumenTypes.DATA);
    }

    /**
     * Resolves the {@link LumenType} for the current field access expression
     * by inspecting the object variable's {@code data_type} metadata and the field name.
     *
     * @param ctx the binding access
     * @return the field type, or {@code null} if it cannot be determined at compile time
     */
    private static @Nullable LumenType resolveFieldLumenType(@NotNull HandlerContext ctx) {
        List<String> objTokens = ctx.tokens("obj");
        if (objTokens.size() != 1) return null;

        TypeEnv.VarHandle varHandle = ctx.env().lookupVar(objTokens.get(0));
        if (varHandle == null || !varHandle.hasMeta("data_type")) return null;

        String dataType = String.valueOf(varHandle.meta("data_type"));
        DataSchema schema = ctx.env().get("data_schema_" + dataType);
        if (schema == null) return null;

        List<String> fieldTokens = ctx.tokens("field");
        if (fieldTokens.size() != 1) return null;

        return schema.fields().get(fieldTokens.get(0));
    }

    @Call
    public void register(@NotNull LumenAPI api) {
        registerConstructor(api);
        registerFieldAccess(api);
    }

    /**
     * Registers the data constructor expression.
     *
     * <p>Syntax: {@code new <type>} or {@code new <type> with field1 val1 field2 val2 ...}
     */
    private void registerConstructor(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("new %body:EXPR%")
                .description("Creates a new data instance. Use 'with' to set fields: new arena with name \"x\" x1 5")
                .examples("set a to new arena", "set a to new arena with name \"PvP\" x1 0 y1 0 z1 0 x2 100 y2 100 z2 100")
                .since("1.0.0")
                .category(Categories.DATA)
                .handler(DataExpressions::handleConstructor));
    }

    private static @NotNull ExpressionResult handleConstructor(@NotNull HandlerContext ctx) {
        List<? extends ScriptToken> bodyTokens = ctx.scriptTokens("body");
        int line = ctx.line();
        String raw = ctx.raw();

        if (bodyTokens.isEmpty()) {
            throw new DiagnosticException(LumenDiagnostic.error("Expected a data type name after 'new'")
                    .at(line, raw)
                    .label("missing data type name")
                    .help("example: new warp")
                    .build());
        }

        ScriptToken typeToken = bodyTokens.get(0);
        String typeName = typeToken.text().toLowerCase();
        TypeEnvImpl env = (TypeEnvImpl) ctx.env();
        DataSchema schema = env.get("data_schema_" + typeName);
        if (schema == null) {
            LumenDiagnostic.Builder diag = LumenDiagnostic.error("Unknown data type '" + typeToken.text() + "'")
                    .at(line, raw)
                    .highlight(typeToken.start(), typeToken.end())
                    .label("no data class declared with this name");
            String closest = FuzzyMatch.closest(typeName, env.dataSchemaNames());
            if (closest != null) diag.help("did you mean '" + closest + "'?");
            else diag.help("declare it with: data " + typeToken.text() + ":");
            throw new DiagnosticException(diag.build());
        }

        ctx.codegen().addImport(DataInstance.class.getName());
        ctx.codegen().addImport(LinkedHashMap.class.getName());
        ctx.codegen().addImport(Map.class.getName());

        if (bodyTokens.size() == 1) {
            if (!schema.fields().isEmpty()) {
                throw new DiagnosticException(LumenDiagnostic.error("Missing fields in '" + typeName + "' construction")
                        .at(line, raw)
                        .highlight(typeToken.start(), typeToken.end())
                        .label("missing: " + String.join(", ", schema.fields().keySet()))
                        .help("example: new " + typeName + " with " + allFieldsExample(schema))
                        .build());
            }
            return new ExpressionResult("new DataInstance(\"" + typeName + "\")", BuiltinLumenTypes.DATA, Map.of("data_type", typeName));
        }

        ScriptToken withToken = bodyTokens.get(1);
        if (!withToken.text().equalsIgnoreCase("with")) {
            throw new DiagnosticException(LumenDiagnostic.error("Expected 'with' after data type name")
                    .at(line, raw)
                    .highlight(withToken.start(), withToken.end())
                    .label("expected 'with', got '" + withToken.text() + "'")
                    .help("example: new " + typeName + " with " + allFieldsExample(schema))
                    .build());
        }

        List<? extends ScriptToken> fieldTokens = bodyTokens.subList(2, bodyTokens.size());
        return buildWithFields(ctx, schema, typeName, fieldTokens, line, raw);
    }

    private static @NotNull ExpressionResult buildWithFields(@NotNull HandlerContext ctx, @NotNull DataSchema schema, @NotNull String typeName, @NotNull List<? extends ScriptToken> tokens, int line, @NotNull String raw) {
        TypeEnvImpl env = (TypeEnvImpl) ctx.env();
        HandlerContextImpl hctxImpl = (HandlerContextImpl) ctx;
        StringJoiner entries = new StringJoiner(", ");
        List<FieldIssue> issues = new ArrayList<>();
        List<DiagnosticException> resolutionFailures = new ArrayList<>();
        Set<String> assigned = new HashSet<>();
        List<String> castHints = new ArrayList<>();

        int i = 0;
        while (i < tokens.size()) {
            ScriptToken fieldToken = tokens.get(i);
            String fieldName = fieldToken.text();

            if (!schema.fields().containsKey(fieldName)) {
                String closest = FuzzyMatch.closest(fieldName, schema.fields().keySet());
                String label = closest != null
                        ? "unknown field, did you mean '" + closest + "'?"
                        : "unknown field of '" + typeName + "'";
                issues.add(new FieldIssue(fieldToken.start(), fieldToken.end(), label));
                i = skipToNextFieldOrEnd(tokens, i + 1, schema);
                continue;
            }

            if (!assigned.add(fieldName)) {
                issues.add(new FieldIssue(fieldToken.start(), fieldToken.end(), "field '" + fieldName + "' is already set"));
                int skipFrom = i + 1;
                int skipEnd = skipToNextFieldOrEnd(tokens, skipFrom, schema);
                i = Math.max(skipEnd, skipFrom + 1);
                continue;
            }

            int valueStart = i + 1;
            if (valueStart >= tokens.size()) {
                issues.add(new FieldIssue(fieldToken.start(), fieldToken.end(), "missing value for '" + fieldName + "'"));
                break;
            }

            ValueResolution attempt = resolveSmallestValue(hctxImpl, tokens, valueStart, tokens.size(), env);

            if (attempt.resolved == null) {
                ScriptToken first = tokens.get(valueStart);
                if (schema.fields().containsKey(first.text())) {
                    issues.add(new FieldIssue(fieldToken.start(), fieldToken.end(), "missing value for '" + fieldName + "'"));
                    i = valueStart;
                    continue;
                }
                int spanEnd = skipToNextFieldOrEnd(tokens, valueStart, schema);
                if (spanEnd <= valueStart) spanEnd = Math.min(valueStart + 1, tokens.size());
                List<? extends ScriptToken> spanTokens = tokens.subList(valueStart, spanEnd);
                List<Token> pipelineValueTokens = HandlerContextImpl.toPipelineTokens(spanTokens);
                List<PatternSimulator.Suggestion> suggestions = PatternSimulator.suggestExpressions(pipelineValueTokens, PatternRegistry.instance(), env);
                LumenDiagnostic diag = !suggestions.isEmpty()
                        ? SuggestionDiagnostics.build("Cannot resolve value for field '" + fieldName + "'", line, raw, pipelineValueTokens, suggestions, env)
                        : SuggestionDiagnostics.buildNoSuggestion("Cannot resolve value for field '" + fieldName + "'", line, raw, pipelineValueTokens, env);
                resolutionFailures.add(new DiagnosticException(diag));
                i = spanEnd;
                continue;
            }

            ResolvedValue resolved = attempt.resolved;
            int valueEnd = attempt.end;
            List<? extends ScriptToken> valueTokens = tokens.subList(valueStart, valueEnd);
            ScriptToken first = valueTokens.get(0);
            ScriptToken last = valueTokens.get(valueTokens.size() - 1);

            LumenType expected = schema.fields().get(fieldName);
            if (!expected.assignableFrom(resolved.type())) {
                issues.add(new FieldIssue(first.start(), last.end(), "field '" + fieldName + "' expects '" + expected.displayName() + "', got '" + resolved.type().displayName() + "'"));
                String castHint = castHintFor(expected, resolved.type(), valueTokens);
                if (castHint != null) castHints.add(castHint);
                i = valueEnd;
                continue;
            }

            entries.add("\"" + fieldName + "\", (Object) " + resolved.java());
            i = valueEnd;
        }

        List<String> missing = new ArrayList<>();
        for (String fieldName : schema.fields().keySet()) {
            if (!assigned.contains(fieldName)) missing.add(fieldName);
        }

        if (!resolutionFailures.isEmpty() && issues.isEmpty() && missing.isEmpty()) {
            throw resolutionFailures.get(0);
        }

        if (!missing.isEmpty() && issues.isEmpty() && resolutionFailures.isEmpty()) {
            StringJoiner sj = new StringJoiner(" ");
            for (String name : missing) sj.add(name).add("<value>");
            throw new DiagnosticException(LumenDiagnostic.error("Missing field" + (missing.size() == 1 ? "" : "s") + " in '" + typeName + "' construction")
                    .at(line, raw)
                    .label("missing: " + String.join(", ", missing))
                    .help("set " + (missing.size() == 1 ? "this field" : "these fields") + " with: " + sj)
                    .build());
        }

        if (!issues.isEmpty()) {
            String title = issues.size() == 1
                    ? "Invalid field in '" + typeName + "' construction"
                    : issues.size() + " invalid fields in '" + typeName + "' construction";
            FieldIssue primary = issues.get(0);
            LumenDiagnostic.Builder diag = LumenDiagnostic.error(title)
                    .at(line, raw)
                    .highlight(primary.start(), primary.end())
                    .label(primary.label());
            for (int idx = 1; idx < issues.size(); idx++) {
                FieldIssue issue = issues.get(idx);
                diag.subHighlight(issue.start(), issue.end(), issue.label());
            }
            diag.help("known fields of '" + typeName + "': " + String.join(", ", schema.fields().keySet()));
            for (String hint : castHints) diag.help(hint);
            if (!missing.isEmpty()) {
                diag.note("also missing: " + String.join(", ", missing));
            }
            if (!resolutionFailures.isEmpty()) {
                diag.note("also failed to resolve " + resolutionFailures.size() + " value expression(s)");
            }
            throw new DiagnosticException(diag.build());
        }

        String mapBuilder = "new LinkedHashMap<>(Map.of(" + entries + "))";
        return new ExpressionResult("new DataInstance(\"" + typeName + "\", " + mapBuilder + ")", BuiltinLumenTypes.DATA, Map.of("data_type", typeName));
    }

    private record FieldIssue(int start, int end, @NotNull String label) {
    }

    private static int skipToNextFieldOrEnd(@NotNull List<? extends ScriptToken> tokens, int from, @NotNull DataSchema schema) {
        int j = from;
        while (j < tokens.size() && !schema.fields().containsKey(tokens.get(j).text())) j++;
        return j;
    }

    private static @NotNull ValueResolution resolveSmallestValue(@NotNull HandlerContextImpl ctx, @NotNull List<? extends ScriptToken> tokens, int start, int maxEnd, @NotNull TypeEnvImpl env) {
        for (int end = start + 1; end <= maxEnd; end++) {
            ResolvedValue rv = resolveValueExpression(ctx, tokens.subList(start, end), env);
            if (rv != null) return new ValueResolution(rv, end);
        }
        return new ValueResolution(null, maxEnd);
    }

    private static @Nullable ResolvedValue resolveValueExpression(@NotNull HandlerContextImpl ctx, @NotNull List<? extends ScriptToken> valueTokens, @NotNull TypeEnvImpl env) {
        List<Token> pipelineTokens = HandlerContextImpl.toPipelineTokens(valueTokens);
        if (pipelineTokens.size() == 1) {
            ResolvedValue lit = resolveSingleTokenLiteral(pipelineTokens.get(0), env);
            if (lit != null) return lit;
        }
        RegisteredExpressionMatch match = PatternRegistry.instance().matchExpression(pipelineTokens, env);
        if (match != null) {
            try {
                HandlerContextImpl hctx = new HandlerContextImpl(match.match(), env, ctx.codegenContext(), env.blockContext(), null, ctx.line(), ctx.raw());
                ExpressionResult result = match.reg().handler().handle(hctx);
                return new ResolvedValue(result.java(), result.type());
            } catch (DiagnosticException e) {
                throw e;
            } catch (RuntimeException ignored) {
            }
        }
        ExpressionResult resolved = ExprResolver.resolveWithType(pipelineTokens, ctx.codegenContext(), env);
        if (resolved != null) return new ResolvedValue(resolved.java(), resolved.type());
        return null;
    }

    private static @Nullable ResolvedValue resolveSingleTokenLiteral(@NotNull Token token, @NotNull TypeEnvImpl env) {
        if (token.kind() == TokenKind.STRING) {
            return new ResolvedValue("\"" + escapeJavaString(token.text()) + "\"", PrimitiveType.STRING);
        }
        if (token.kind() == TokenKind.NUMBER) {
            String text = token.text();
            if (text.contains(".")) return new ResolvedValue(text, PrimitiveType.DOUBLE);
            return new ResolvedValue(text, PrimitiveType.INT);
        }
        if (token.kind() == TokenKind.IDENT) {
            String text = token.text();
            if (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false")) {
                return new ResolvedValue(text.toLowerCase(), PrimitiveType.BOOLEAN);
            }
            VarRef ref = env.lookupVar(text);
            if (ref != null) return new ResolvedValue(ref.java(), ref.type());
        }
        return null;
    }

    private static @Nullable String castHintFor(@NotNull LumenType expected, @NotNull LumenType actual, @NotNull List<? extends ScriptToken> valueTokens) {
        if (actual.unwrap() != PrimitiveType.STRING) return null;
        LumenType target = expected.unwrap();
        String castWord;
        if (target == PrimitiveType.INT || target == PrimitiveType.LONG) castWord = "as integer";
        else if (target == PrimitiveType.DOUBLE || target == PrimitiveType.FLOAT) castWord = "as number";
        else return null;
        StringJoiner sj = new StringJoiner(" ");
        for (ScriptToken t : valueTokens) sj.add(t.text());
        return "convert string to " + target.displayName() + " with '" + sj + " " + castWord + "'";
    }

    private static @NotNull String escapeJavaString(@NotNull String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    private record ResolvedValue(@NotNull String java, @NotNull LumenType type) {
    }

    private record ValueResolution(@Nullable ResolvedValue resolved, int end) {
    }

    private static @NotNull String allFieldsExample(@NotNull DataSchema schema) {
        StringJoiner sj = new StringJoiner(" ");
        for (String name : schema.fields().keySet()) sj.add(name).add("<value>");
        return sj.toString();
    }

    /**
     * Registers field access expression patterns.
     *
     * <p>When the data type and field name are both known at compile time, the returned
     * expression is cast to the schema's declared {@link LumenType}. Otherwise the raw
     * {@code Object} value from {@code DataInstance.get()} is returned as DATA type.
     */
    private void registerFieldAccess(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("get field %field:STRING% (of|from) %obj:DATA%")
                .description("Gets a field value from a data instance.")
                .examples("set name to get field \"name\" of myArena", "set x to get field \"x1\" from myArena")
                .since("1.0.0")
                .category(Categories.DATA)
                .handler(ctx -> {
                    ctx.codegen().addImport(DataInstance.class.getName());
                    String objJava = ctx.java("obj");
                    String fieldJava = ctx.java("field");
                    String rawGet = "((DataInstance) " + objJava + ").get(" + fieldJava + ")";
                    return typedFieldResult(ctx, rawGet);
                }));

        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("%obj:DATA% field [of] %field:STRING%")
                .description("Gets a field value from a data instance using postfix syntax.")
                .example("set name to myArena field \"name\"")
                .since("1.0.0")
                .category(Categories.DATA)
                .handler(ctx -> {
                    ctx.codegen().addImport(DataInstance.class.getName());
                    String objJava = ctx.java("obj");
                    String fieldJava = ctx.java("field");
                    String rawGet = "((DataInstance) " + objJava + ").get(" + fieldJava + ")";
                    return typedFieldResult(ctx, rawGet);
                }));
    }
}
