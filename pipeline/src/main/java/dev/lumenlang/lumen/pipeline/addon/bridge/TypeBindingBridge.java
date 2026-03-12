package dev.lumenlang.lumen.pipeline.addon.bridge;

import dev.lumenlang.lumen.api.type.AddonTypeBinding;
import dev.lumenlang.lumen.pipeline.codegen.CodegenContext;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.language.TypeBinding;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;

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
    public Object parse(@NotNull List<Token> tokens, @NotNull TypeEnv env) {
        List<String> texts = tokens.stream()
                .map(Token::text)
                .collect(Collectors.toList());
        return apiBinding.parse(texts, env);
    }

    @Override
    public @NotNull String toJava(Object value, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
        return apiBinding.toJava(value, ctx, env);
    }

    @Override
    public int consumeCount(@NotNull List<Token> tokens, @NotNull TypeEnv env) {
        List<String> texts = tokens.stream()
                .map(Token::text)
                .collect(Collectors.toList());
        return apiBinding.consumeCount(texts, env);
    }
}
