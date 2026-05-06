package dev.lumenlang.lumen.plugin.defaults.expression;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.pipeline.codegen.HandlerContextImpl;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.language.simulator.suggestions.SuggestionDiagnostics;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.type.ParseResult;
import dev.lumenlang.lumen.pipeline.type.ParsedType;
import dev.lumenlang.lumen.pipeline.type.TypeAnnotationParser;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Registers the {@code nullable <type>} expression that produces a typed null value.
 */
@Registration
@SuppressWarnings("unused")
public final class NullableExpressions {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.patterns().expression(b -> b
                .by("Lumen")
                .pattern("nullable %t:EXPR%")
                .description("Produces a typed null value carrying the given type at compile time.")
                .examples("set target to nullable player", "pos1: nullable location with default nullable location")
                .since("1.0.0")
                .category(Categories.VARIABLE)
                .handler(ctx -> {
                    HandlerContextImpl hctx = (HandlerContextImpl) ctx;
                    TypeEnvImpl env = (TypeEnvImpl) ctx.env();
                    List<Token> typeTokens = hctx.bound("t").tokens();
                    ParseResult result = TypeAnnotationParser.parse(typeTokens, 0, env::lookupDataSchema);
                    if (!result.ok()) {
                        throw new DiagnosticException(SuggestionDiagnostics.buildTypeFailure("Invalid nullable type", ctx.source().currentLine(), ctx.source().currentRaw(), typeTokens, result));
                    }
                    ParsedType parsed = result.parsed();
                    LumenType type = parsed.type();
                    String javaType = type.javaType();
                    String simple = javaType.contains(".") ? javaType.substring(javaType.lastIndexOf('.') + 1) : javaType;
                    if (javaType.contains(".")) ctx.codegen().addImport(javaType);
                    return new ExpressionResult("(" + simple + ") null", type.wrapAsNullable());
                }));
    }
}
