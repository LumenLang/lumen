package net.vansencool.lumen.pipeline.language.typed;

import net.vansencool.lumen.pipeline.codegen.TypeEnv;
import net.vansencool.lumen.pipeline.language.tokenization.Token;
import net.vansencool.lumen.pipeline.language.tokenization.TokenKind;
import net.vansencool.lumen.pipeline.math.MathEngine;
import net.vansencool.lumen.pipeline.placeholder.PlaceholderExpander;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Parses a list of tokens into an {@link Expr} variant.
 *
 * <p>
 * The parser tries each expression form in priority order:
 * <ol>
 * <li>Single token: string literal, numeric literal, or variable reference</li>
 * <li>Single placeholder: {@code {name}} resolved via the placeholder
 * system</li>
 * <li>Math expression: any token sequence recognised by {@link MathEngine}</li>
 * <li>Fallback: wrap as {@link Expr.RawExpr} for downstream handling</li>
 * </ol>
 *
 * @see Expr
 * @see StatementClassifier
 */
public final class ExprParser {

    /**
     * Parses the given tokens into the most specific {@link Expr} variant possible.
     *
     * @param tokens the tokens forming the expression (must not be empty)
     * @param env    the type environment for variable lookups
     * @return the parsed expression
     * @throws RuntimeException if the token list is empty
     */
    public static @NotNull Expr parse(@NotNull List<Token> tokens, @NotNull TypeEnv env) {
        if (tokens.isEmpty())
            throw new RuntimeException("Empty expression");

        if (tokens.size() == 1) {
            Token t = tokens.get(0);

            if (t.kind() == TokenKind.STRING)
                return new Expr.Literal(t.text());

            if (t.kind() == TokenKind.NUMBER)
                return new Expr.Literal(Integer.parseInt(t.text()));

            if (t.kind() == TokenKind.IDENT) {
                String text = t.text();
                if (text.equalsIgnoreCase("true") || text.equalsIgnoreCase("false"))
                    return new Expr.Literal(Boolean.parseBoolean(text.toLowerCase()));

                if (env.lookupVar(text) != null)
                    return new Expr.RefExpr(text);
            }
        }

        if (isSinglePlaceholder(tokens)) {
            String placeholder = tokens.get(1).text();
            String java = PlaceholderExpander.resolveForExpression(placeholder, env);
            if (java != null) {
                return new Expr.MathExpr(java);
            }
        }

        if (MathEngine.isMathExpression(tokens, env)) {
            return new Expr.MathExpr(MathEngine.compile(tokens, env));
        }

        return new Expr.RawExpr(tokens);
    }

    /**
     * Checks whether the token list is exactly a single placeholder:
     * {@code { ident }}.
     */
    private static boolean isSinglePlaceholder(@NotNull List<Token> tokens) {
        return tokens.size() == 3
                && tokens.get(0).kind() == TokenKind.SYMBOL && tokens.get(0).text().equals("{")
                && tokens.get(1).kind() == TokenKind.IDENT
                && tokens.get(2).kind() == TokenKind.SYMBOL && tokens.get(2).text().equals("}");
    }
}
