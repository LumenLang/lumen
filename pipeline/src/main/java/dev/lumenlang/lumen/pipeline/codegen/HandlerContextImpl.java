package dev.lumenlang.lumen.pipeline.codegen;

import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.codegen.JavaOutput;
import dev.lumenlang.lumen.api.emit.ScriptToken;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.pipeline.conditions.ConditionExpr;
import dev.lumenlang.lumen.pipeline.conditions.parser.ConditionParser;
import dev.lumenlang.lumen.pipeline.language.exceptions.TokenCarryingException;
import dev.lumenlang.lumen.pipeline.language.match.BoundValue;
import dev.lumenlang.lumen.pipeline.language.match.BraceExpr;
import dev.lumenlang.lumen.pipeline.language.match.InlineExpr;
import dev.lumenlang.lumen.pipeline.language.match.Match;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.resolve.ExprResolver;
import dev.lumenlang.lumen.pipeline.language.resolve.PatternSimulator;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.language.tokenization.TokenKind;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Pipeline-internal implementation of {@link HandlerContext}.
 *
 * <p>
 * Combines pattern match binding access with emit-time context (output builder,
 * source line information, and expression resolution). This is the single context
 * object that all handler types receive during code generation.
 *
 * <p>
 * When constructed without a {@link Match} (e.g. for form handlers or hooks),
 * the pattern binding methods will throw if called.
 *
 * @see HandlerContext
 */
@SuppressWarnings("unused")
public final class HandlerContextImpl implements HandlerContext {

    private final @Nullable Match match;
    private final TypeEnv env;
    private final CodegenContext ctx;
    private final @Nullable BlockContext block;
    private final @Nullable JavaOutput out;
    private final int line;
    private final @NotNull String raw;

    public HandlerContextImpl(@Nullable Match match, @NotNull TypeEnv env, @NotNull CodegenContext ctx, @Nullable BlockContext block, @Nullable JavaOutput out, int line, @NotNull String raw) {
        this.match = match;
        this.env = env;
        this.ctx = ctx;
        this.block = block;
        this.out = out;
        this.line = line;
        this.raw = raw;
    }

    /**
     * Converts a list of API-level {@link ScriptToken}s to pipeline-internal {@link Token}s.
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
                result.add(new Token(mapTokenType(t.tokenType()), t.text(), t.line(), t.start(), t.end()));
            }
        }
        return result;
    }

    private static @NotNull TokenKind mapTokenType(ScriptToken.@NotNull TokenType type) {
        return switch (type) {
            case IDENT -> TokenKind.IDENT;
            case NUMBER -> TokenKind.NUMBER;
            case STRING -> TokenKind.STRING;
            case SYMBOL -> TokenKind.SYMBOL;
        };
    }

    /**
     * Returns the internal {@link CodegenContext} for handlers that need pipeline-level access.
     *
     * @return the codegen context
     */
    public @NotNull CodegenContext codegenContext() {
        return ctx;
    }

    private @NotNull Match requireMatch() {
        if (match == null) throw new IllegalStateException("No pattern match available in this context");
        return match;
    }

    /**
     * Retrieves the bound value for the specified parameter name.
     *
     * @param n the parameter name from the pattern (e.g., "who", "item", "amt")
     * @return the bound value containing parse results and metadata
     */
    public BoundValue bound(@NotNull String n) {
        return requireMatch().values().get(n);
    }

    /**
     * Returns the bound value at the given positional index.
     *
     * @param index the zero-based index
     * @return the bound value
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public BoundValue boundAt(int index) {
        return requireMatch().boundAt(index);
    }

    @Override
    public @NotNull String java(@NotNull String name) {
        LumenLogger.debug("HandlerContextImpl", "java() called for parameter: '" + name + "'");
        BoundValue v = bound(name);
        if (v == null) {
            LumenLogger.debug("HandlerContextImpl", "ERROR: BoundValue is null for '" + name + "'");
            throw new RuntimeException("No bound value for parameter: " + name);
        }
        LumenLogger.debug("HandlerContextImpl", "BoundValue found: value=" + v.value() + ", binding=" + v.binding().id());
        String result = requireMatch().java(name, ctx, env);
        LumenLogger.debug("HandlerContextImpl", "Generated Java for '" + name + "': '" + result + "'");
        return result;
    }

    @Override
    public @Nullable Object value(@NotNull String name) {
        return resolveDeferred(bound(name).value());
    }

    @Override
    public @NotNull List<String> tokens(@NotNull String name) {
        BoundValue bv = bound(name);
        return bv.tokens().stream().map(Token::text).collect(Collectors.toList());
    }

    @Override
    public @NotNull TypeEnv env() {
        return env;
    }

    @Override
    public @NotNull CodegenContext codegen() {
        return ctx;
    }

    @Override
    public @NotNull BlockContext block() {
        if (block == null) throw new IllegalStateException("No block context available");
        return block;
    }

    @Override
    public @NotNull JavaOutput out() {
        if (out == null) throw new IllegalStateException("No output available in this context");
        return out;
    }

    @Override
    public int line() {
        return line;
    }

    @Override
    public @NotNull String raw() {
        return raw;
    }

    @Override
    public @Nullable String choice(int index) {
        return requireMatch().choice(index);
    }

    @Override
    public @NotNull String java(int index) {
        return requireMatch().javaAt(index, ctx, env);
    }

    @Override
    public @NotNull Object value(int index) {
        return resolveDeferred(requireMatch().valueAt(index));
    }

    @Override
    public @NotNull List<String> tokens(int index) {
        BoundValue bv = requireMatch().boundAt(index);
        return bv.tokens().stream().map(Token::text).collect(Collectors.toList());
    }

    @Override
    public int size() {
        return match != null ? match.size() : 0;
    }

    @Override
    public @Nullable LumenType resolvedType(@NotNull String name) {
        Object val = value(name);
        if (val instanceof EnvironmentAccess.VarHandle vh) return vh.type();
        BoundValue bv = bound(name);
        if (bv == null) return null;
        ExpressionResult result = ExprResolver.resolveWithType(bv.tokens(), ctx, env);
        return result != null ? result.type() : null;
    }

    @Override
    public @Nullable String resolveExpression(@NotNull List<? extends ScriptToken> tokens) {
        List<Token> pipelineTokens = toPipelineTokens(tokens);
        return ExprResolver.resolve(pipelineTokens, ctx, env);
    }

    @Override
    public @NotNull String parseCondition(@NotNull String paramName) {
        List<Token> tokens = bound(paramName).tokens();
        ConditionParser cp = new ConditionParser(PatternRegistry.instance().conditionRegistry());
        ConditionExpr expr = cp.parse(tokens, env);
        try {
            return expr.toJava(env, ctx);
        } catch (TokenCarryingException e) {
            List<PatternSimulator.Suggestion> suggestions = PatternSimulator.suggestConditions(tokens, PatternRegistry.instance(), env);
            if (!suggestions.isEmpty()) {
                throw new TokenCarryingException("Unknown condition: " + ExprResolver.joinTokens(tokens), tokens, suggestions);
            }
            throw new TokenCarryingException(e.getMessage(), tokens);
        } catch (RuntimeException e) {
            List<PatternSimulator.Suggestion> suggestions = PatternSimulator.suggestConditions(tokens, PatternRegistry.instance(), env);
            if (!suggestions.isEmpty()) {
                throw new TokenCarryingException("Unknown condition: " + ExprResolver.joinTokens(tokens), tokens, suggestions);
            }
            throw new TokenCarryingException(e.getMessage() != null ? e.getMessage() : "Condition failed", tokens);
        }
    }

    private @NotNull Object resolveDeferred(@NotNull Object value) {
        if (value instanceof InlineExpr ie) {
            ExpressionResult result = ExprResolver.resolveWithType(ie.tokens(), ctx, env);
            if (result != null) return toSyntheticHandle(result);
        }
        if (value instanceof BraceExpr be) {
            ExpressionResult result = ExprResolver.resolveWithType(be.innerTokens(), ctx, env);
            if (result != null) return toSyntheticHandle(result);
        }
        return value;
    }

    private static EnvironmentAccess.@NotNull VarHandle toSyntheticHandle(@NotNull ExpressionResult result) {
        return Match.syntheticHandle(result.java(), result.type(), result.metadata());
    }
}
