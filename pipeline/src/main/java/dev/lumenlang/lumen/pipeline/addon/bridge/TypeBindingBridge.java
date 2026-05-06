package dev.lumenlang.lumen.pipeline.addon.bridge;

import dev.lumenlang.lumen.api.language.SemanticKind;
import dev.lumenlang.lumen.api.language.Suggestion;
import dev.lumenlang.lumen.api.type.AddonTypeBinding;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.pipeline.codegen.CodegenContextImpl;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.language.TypeBinding;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Adapts an API-level {@link AddonTypeBinding} into an internal {@link TypeBinding}.
 */
public final class TypeBindingBridge implements TypeBinding {

    private final AddonTypeBinding apiBinding;

    public TypeBindingBridge(@NotNull AddonTypeBinding apiBinding) {
        this.apiBinding = apiBinding;
    }

    @Override
    public @NotNull String id() {
        return apiBinding.id();
    }

    @Override
    public Object parse(@NotNull List<Token> tokens, @NotNull TypeEnvImpl env) {
        List<String> texts = tokens.stream()
                .map(Token::text)
                .collect(Collectors.toList());
        return apiBinding.parse(texts, env);
    }

    @Override
    public @NotNull String toJava(Object value, @NotNull CodegenContextImpl ctx, @NotNull TypeEnvImpl env) {
        return apiBinding.toJava(value, ctx, env);
    }

    @Override
    public int consumeCount(@NotNull List<Token> tokens, @NotNull TypeEnvImpl env) {
        List<String> texts = tokens.stream()
                .map(Token::text)
                .collect(Collectors.toList());
        return apiBinding.consumeCount(texts, env);
    }

    @Override
    public @NotNull SemanticKind semanticKind() {
        return apiBinding.semanticKind();
    }

    @Override
    public @NotNull List<Suggestion> suggestions(@NotNull TypeEnvImpl env, @Nullable LumenType expectedType) {
        return apiBinding.suggestions(env, expectedType);
    }
}
