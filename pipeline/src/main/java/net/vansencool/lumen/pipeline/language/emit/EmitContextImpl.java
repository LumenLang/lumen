package net.vansencool.lumen.pipeline.language.emit;

import net.vansencool.lumen.api.codegen.CodegenAccess;
import net.vansencool.lumen.api.codegen.EnvironmentAccess;
import net.vansencool.lumen.api.codegen.JavaOutput;
import net.vansencool.lumen.api.emit.EmitContext;
import net.vansencool.lumen.api.emit.ScriptToken;
import net.vansencool.lumen.pipeline.codegen.CodegenContext;
import net.vansencool.lumen.pipeline.codegen.TypeEnv;
import net.vansencool.lumen.pipeline.java.JavaBuilder;
import net.vansencool.lumen.pipeline.language.resolve.ExprResolver;
import net.vansencool.lumen.pipeline.language.tokenization.Token;
import net.vansencool.lumen.pipeline.language.tokenization.TokenKind;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Pipeline-internal implementation of {@link EmitContext}.
 *
 * <p>Wraps the internal {@link TypeEnv}, {@link CodegenContext}, and {@link JavaBuilder}
 * and exposes them through the API-level interfaces.
 */
public final class EmitContextImpl implements EmitContext {

    private final TypeEnv env;
    private final CodegenContext ctx;
    private final JavaBuilder out;
    private final int line;
    private final String raw;

    public EmitContextImpl(@NotNull TypeEnv env, @NotNull CodegenContext ctx,
                           @NotNull JavaBuilder out, int line, @NotNull String raw) {
        this.env = env;
        this.ctx = ctx;
        this.out = out;
        this.line = line;
        this.raw = raw;
    }

    @Override
    public @NotNull EnvironmentAccess env() {
        return env;
    }

    @Override
    public @NotNull CodegenAccess codegen() {
        return ctx;
    }

    @Override
    public @NotNull JavaOutput out() {
        return out;
    }

    @Override
    public @Nullable String resolveExpression(@NotNull List<? extends ScriptToken> tokens) {
        List<Token> pipelineTokens = toPipelineTokens(tokens);
        return ExprResolver.resolve(pipelineTokens, ctx, env);
    }

    @Override
    public int line() {
        return line;
    }

    @Override
    public @NotNull String raw() {
        return raw;
    }

    /**
     * Returns the internal {@link CodegenContext} for handlers that need pipeline-level access.
     *
     * @return the codegen context
     */
    public @NotNull CodegenContext codegenContext() {
        return ctx;
    }

    /**
     * Converts a list of API-level {@link ScriptToken}s to pipeline-internal {@link Token}s.
     *
     * <p>If the tokens are already {@link Token} instances, they are cast directly.
     * Otherwise, a new {@link Token} is constructed with a mapped {@link TokenKind}.
     *
     * @param tokens the API-level tokens
     * @return the pipeline-internal tokens
     */
    public static @NotNull List<Token> toPipelineTokens(@NotNull List<? extends ScriptToken> tokens) {
        List<Token> result = new ArrayList<>(tokens.size());
        for (ScriptToken t : tokens) {
            if (t instanceof Token pt) {
                result.add(pt);
            } else {
                result.add(new Token(
                        mapTokenType(t.tokenType()),
                        t.text(), t.line(), t.start(), t.end()));
            }
        }
        return result;
    }

    private static @NotNull TokenKind mapTokenType(
            ScriptToken.@NotNull TokenType type) {
        return switch (type) {
            case IDENT -> TokenKind.IDENT;
            case NUMBER -> TokenKind.NUMBER;
            case STRING -> TokenKind.STRING;
            case SYMBOL -> TokenKind.SYMBOL;
        };
    }
}
