package net.vansencool.lumen.pipeline.language.resolve;

import net.vansencool.lumen.api.handler.ExpressionHandler.ExpressionResult;
import net.vansencool.lumen.pipeline.codegen.BindingContext;
import net.vansencool.lumen.pipeline.codegen.BlockContext;
import net.vansencool.lumen.pipeline.codegen.CodegenContext;
import net.vansencool.lumen.pipeline.codegen.TypeEnv;
import net.vansencool.lumen.pipeline.language.pattern.PatternRegistry;
import net.vansencool.lumen.pipeline.language.pattern.RegisteredExpressionMatch;
import net.vansencool.lumen.pipeline.language.tokenization.Token;
import net.vansencool.lumen.pipeline.language.tokenization.TokenKind;
import net.vansencool.lumen.pipeline.var.RefType;
import net.vansencool.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves token lists into Java expressions by trying, in order:
 * direct expression pattern matching, recursive sub-expression resolution,
 * and arithmetic splitting.
 */
public final class ExprResolver {

    private static final int MAX_RESOLVE_DEPTH = 10;

    private ExprResolver() {
    }

    /**
     * Resolves a list of tokens into a Java expression.
     *
     * @param tokens the tokens to resolve
     * @param ctx    the code generation context
     * @param env    the type environment
     * @return the generated Java source expression, or null if not resolvable
     */
    public static @Nullable String resolve(
            @NotNull List<Token> tokens,
            @NotNull CodegenContext ctx,
            @NotNull TypeEnv env) {
        return resolveRecursive(tokens, ctx, env, 0);
    }

    /**
     * Joins a list of tokens into a single space-separated string.
     *
     * @param tokens the tokens to join
     * @return a space-separated string of token texts
     */
    public static @NotNull String joinTokens(@NotNull List<Token> tokens) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            if (i > 0)
                sb.append(' ');
            sb.append(tokens.get(i).text());
        }
        return sb.toString();
    }

    /**
     * Recursive implementation of expression token resolution. Tries three
     * strategies in order: (1) direct expression match, (2) nested sub-expression
     * substitution, and (3) arithmetic operator splitting.
     *
     * @param tokens the tokens to resolve
     * @param ctx    the code generation context
     * @param env    the type environment
     * @param depth  the current recursion depth (capped at {@link #MAX_RESOLVE_DEPTH})
     * @return the generated Java source expression, or null if not resolvable
     */
    private static @Nullable String resolveRecursive(
            @NotNull List<Token> tokens,
            @NotNull CodegenContext ctx,
            @NotNull TypeEnv env,
            int depth) {
        if (depth > MAX_RESOLVE_DEPTH || tokens.isEmpty()) return null;

        if (tokens.size() >= 3
                && tokens.get(0).kind() == TokenKind.SYMBOL
                && tokens.get(0).text().equals("{")
                && tokens.get(tokens.size() - 1).kind() == TokenKind.SYMBOL
                && tokens.get(tokens.size() - 1).text().equals("}")) {
            return resolveRecursive(
                    tokens.subList(1, tokens.size() - 1), ctx, env, depth + 1);
        }

        PatternRegistry reg;
        try {
            reg = PatternRegistry.instance();
        } catch (RuntimeException e) {
            return null;
        }

        RegisteredExpressionMatch match = reg.matchExpression(tokens, env);
        if (match != null) {
            try {
                BlockContext block = env.blockContext();
                BindingContext bc = new BindingContext(match.match(), env, ctx, block);
                ExpressionResult result = match.reg().handler().handle(bc);
                return result.java();
            } catch (RuntimeException ignored) {
            }
        }

        String nested = tryNestedSubExpressions(tokens, ctx, env, reg, depth);
        if (nested != null) return nested;

        return tryMathSplit(tokens, ctx, env, depth);
    }

    /**
     * Attempts to resolve tokens by finding a contiguous sub-range that matches
     * a registered expression, substituting it with a synthetic variable, and
     * retrying on the simplified token list.
     *
     * @param tokens the full token list
     * @param ctx    the code generation context
     * @param env    the type environment
     * @param reg    the pattern registry
     * @param depth  the current recursion depth
     * @return the resolved Java expression, or null
     */
    private static @Nullable String tryNestedSubExpressions(
            @NotNull List<Token> tokens,
            @NotNull CodegenContext ctx,
            @NotNull TypeEnv env,
            @NotNull PatternRegistry reg,
            int depth) {
        for (int start = 0; start < tokens.size(); start++) {
            for (int end = tokens.size(); end > start + 1; end--) {
                if (start == 0 && end == tokens.size()) continue;

                List<Token> sub = tokens.subList(start, end);
                RegisteredExpressionMatch subMatch = reg.matchExpression(sub, env);
                if (subMatch == null) continue;

                ExpressionResult subResult;
                try {
                    BlockContext block = env.blockContext();
                    BindingContext bc = new BindingContext(subMatch.match(), env, ctx, block);
                    subResult = subMatch.reg().handler().handle(bc);
                } catch (RuntimeException e) {
                    continue;
                }

                if (subResult.refTypeId() == null) continue;

                String synthName = "$sub" + depth + "$" + start;
                RefType refType = RefType.byId(subResult.refTypeId());
                VarRef synthRef = new VarRef(refType, subResult.java());

                BlockContext tempBlock = new BlockContext(null, env.blockContext(), List.of(), 0);
                env.enterBlock(tempBlock);
                env.defineVar(synthName, synthRef);

                try {
                    List<Token> newTokens = new ArrayList<>(tokens.subList(0, start));
                    newTokens.add(new Token(TokenKind.IDENT, synthName,
                            sub.get(0).line(), sub.get(0).start(), sub.get(0).end()));
                    newTokens.addAll(tokens.subList(end, tokens.size()));

                    String resolved = resolveRecursive(newTokens, ctx, env, depth + 1);
                    if (resolved != null) return resolved;
                } finally {
                    env.leaveBlock();
                }
            }
        }
        return null;
    }

    /**
     * Attempts to parse tokens as a math expression with parenthesis support.
     * Splits only at top-level arithmetic operators (not inside parentheses)
     * and resolves each operand recursively.
     *
     * @param tokens the token list to parse
     * @param ctx    the code generation context
     * @param env    the type environment
     * @param depth  the current recursion depth
     * @return the compiled Java math expression, or null if not resolvable
     */
    private static @Nullable String tryMathSplit(
            @NotNull List<Token> tokens,
            @NotNull CodegenContext ctx,
            @NotNull TypeEnv env,
            int depth) {
        List<List<Token>> operands = new ArrayList<>();
        List<String> operators = new ArrayList<>();
        List<Token> current = new ArrayList<>();
        int parenDepth = 0;

        for (Token t : tokens) {
            if (t.kind() == TokenKind.SYMBOL && t.text().equals("(")) {
                parenDepth++;
                current.add(t);
            } else if (t.kind() == TokenKind.SYMBOL && t.text().equals(")")) {
                parenDepth--;
                current.add(t);
            } else if (parenDepth == 0 && t.kind() == TokenKind.SYMBOL
                    && isArithmeticOp(t.text()) && !current.isEmpty()) {
                operands.add(current);
                operators.add(t.text());
                current = new ArrayList<>();
            } else {
                current.add(t);
            }
        }
        if (!current.isEmpty()) operands.add(current);

        if (operators.isEmpty() || operands.size() != operators.size() + 1) return null;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < operands.size(); i++) {
            if (i > 0) sb.append(' ').append(operators.get(i - 1)).append(' ');
            String resolved = resolveMathOperand(operands.get(i), ctx, env, depth);
            if (resolved == null) return null;
            sb.append(resolved);
        }
        return sb.toString();
    }

    /**
     * Resolves a single operand within a math expression.
     *
     * @param tokens the operand tokens
     * @param ctx    the code generation context
     * @param env    the type environment
     * @param depth  the current recursion depth
     * @return the resolved Java expression for this operand, or null
     */
    private static @Nullable String resolveMathOperand(
            @NotNull List<Token> tokens,
            @NotNull CodegenContext ctx,
            @NotNull TypeEnv env,
            int depth) {
        if (tokens.isEmpty()) return null;

        if (tokens.size() == 1) {
            Token t = tokens.get(0);
            if (t.kind() == TokenKind.NUMBER) return t.text();
            if (t.kind() == TokenKind.IDENT) {
                VarRef ref = env.lookupVar(t.text());
                if (ref != null) return ref.java();
            }
            return null;
        }

        if (tokens.size() >= 3
                && tokens.get(0).kind() == TokenKind.SYMBOL && tokens.get(0).text().equals("(")
                && tokens.get(tokens.size() - 1).kind() == TokenKind.SYMBOL
                && tokens.get(tokens.size() - 1).text().equals(")")) {
            List<Token> inner = tokens.subList(1, tokens.size() - 1);
            String innerResolved = resolveRecursive(inner, ctx, env, depth + 1);
            if (innerResolved != null) return "(" + innerResolved + ")";
            return null;
        }

        return resolveRecursive(tokens, ctx, env, depth + 1);
    }

    private static boolean isArithmeticOp(@NotNull String s) {
        return s.equals("+") || s.equals("-") || s.equals("*") || s.equals("/");
    }
}
