package dev.lumenlang.lumen.pipeline.language;

import dev.lumenlang.lumen.pipeline.codegen.CodegenContext;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.java.JavaBuilder;
import dev.lumenlang.lumen.pipeline.language.compile.PatternCompiler;
import dev.lumenlang.lumen.pipeline.language.emit.CodeEmitter;
import dev.lumenlang.lumen.pipeline.language.match.Match;
import dev.lumenlang.lumen.pipeline.language.match.PatternMatcher;
import dev.lumenlang.lumen.pipeline.language.nodes.Node;
import dev.lumenlang.lumen.pipeline.language.parse.LumenParser;
import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.resolve.ExprResolver;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.typebinding.TypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Thin facade that delegates to the focused implementation classes.
 *
 * <p>All actual logic lives in:
 * <ul>
 * <li>{@link PatternCompiler} for pattern compilation</li>
 * <li>{@link PatternMatcher} for pattern matching (with backtracking protection)</li>
 * <li>{@link CodeEmitter} for AST walking and Java code generation</li>
 * <li>{@link ExprResolver} for expression token resolution</li>
 * <li>{@link LumenParser} for indentation-based parsing</li>
 * </ul>
 *
 * <p>This class exists for backward compatibility. New code should reference
 * the implementation classes directly.
 *
 * @see PatternCompiler
 * @see PatternMatcher
 * @see CodeEmitter
 * @see ExprResolver
 */
@SuppressWarnings("unused")
public final class LumenCore {

    private LumenCore() {
    }

    /**
     * Compiles a pattern string into a structured {@link Pattern} object.
     *
     * @param raw the pattern string to compile
     * @return a compiled Pattern
     * @see PatternCompiler#compile(String)
     */
    public static @NotNull Pattern compile(@NotNull String raw) {
        return PatternCompiler.compile(raw);
    }

    /**
     * Attempts to match a list of tokens against a pattern.
     *
     * @param tokens the input tokens
     * @param p      the pattern to match
     * @param types  the type registry
     * @param env    the type environment
     * @return a {@link Match} or null if matching failed
     * @see PatternMatcher#match(List, Pattern, TypeRegistry, TypeEnv)
     */
    public static @Nullable Match match(
            @NotNull List<Token> tokens,
            @NotNull Pattern p,
            @NotNull TypeRegistry types,
            @NotNull TypeEnv env) {
        return PatternMatcher.match(tokens, p, types, env);
    }

    /**
     * Convenience overload that tokenizes, parses, and generates from raw source.
     *
     * @see CodeEmitter#generate(String, PatternRegistry, TypeEnv, CodegenContext, JavaBuilder)
     */
    public static void generate(
            @NotNull String src,
            @NotNull PatternRegistry reg,
            @NotNull TypeEnv env,
            @NotNull CodegenContext ctx,
            @NotNull JavaBuilder out) {
        CodeEmitter.generate(src, reg, env, ctx, out);
    }

    /**
     * Walks the parsed AST and generates Java source code.
     *
     * @see CodeEmitter#generate(Node, PatternRegistry, TypeEnv, CodegenContext, JavaBuilder)
     */
    public static void generate(
            @NotNull Node root,
            @NotNull PatternRegistry reg,
            @NotNull TypeEnv env,
            @NotNull CodegenContext ctx,
            @NotNull JavaBuilder out) {
        CodeEmitter.generate(root, reg, env, ctx, out);
    }

    /**
     * Resolves a list of tokens into a Java expression.
     *
     * @see ExprResolver#resolve(List, CodegenContext, TypeEnv)
     */
    public static @Nullable String resolveExprTokens(
            @NotNull List<Token> tokens,
            @NotNull CodegenContext ctx,
            @NotNull TypeEnv env) {
        return ExprResolver.resolve(tokens, ctx, env);
    }

    /**
     * Joins a list of tokens into a single space-separated string.
     *
     * @see ExprResolver#joinTokens(List)
     */
    public static @NotNull String joinTokens(@NotNull List<Token> tokens) {
        return ExprResolver.joinTokens(tokens);
    }
}
