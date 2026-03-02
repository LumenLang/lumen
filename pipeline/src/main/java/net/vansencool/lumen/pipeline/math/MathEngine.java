package net.vansencool.lumen.pipeline.math;

import net.vansencool.lumen.api.placeholder.PlaceholderType;
import net.vansencool.lumen.pipeline.codegen.TypeEnv;
import net.vansencool.lumen.pipeline.language.tokenization.Token;
import net.vansencool.lumen.pipeline.language.tokenization.TokenKind;
import net.vansencool.lumen.pipeline.placeholder.PlaceholderExpander;
import net.vansencool.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A simple recursive-descent math expression parser that compiles a list of
 * {@link Token}s into a Java source expression respecting standard operator precedence.
 *
 * <p>Supported operators (in order of precedence, lowest to highest):
 * <ol>
 *   <li>{@code +} and {@code -} (additive)</li>
 *   <li>{@code *} and {@code /} (multiplicative)</li>
 *   <li>Parenthesised sub-expressions</li>
 * </ol>
 *
 * <p>Operands may be integer literals ({@link TokenKind#NUMBER}), variable
 * references ({@link TokenKind#IDENT}) that are resolved against a {@link TypeEnv},
 * or placeholder expressions like {@code {player_y}} that are resolved via
 * {@link PlaceholderExpander}.
 */
public final class MathEngine {

    private final List<Token> tokens;
    private final TypeEnv env;
    private int pos;

    private MathEngine(@NotNull List<Token> tokens, @NotNull TypeEnv env) {
        this.tokens = tokens;
        this.env = env;
        this.pos = 0;
    }

    /**
     * Compiles a list of tokens into a Java expression string.
     *
     * @param tokens the token list representing the math expression
     * @param env    the compile-time symbol table for variable resolution
     * @return a Java source expression string
     * @throws RuntimeException if the expression is malformed or references an unknown variable
     */
    public static @NotNull String compile(@NotNull List<Token> tokens, @NotNull TypeEnv env) {
        MathEngine engine = new MathEngine(tokens, env);
        String result = engine.parseExpr();
        if (engine.pos < engine.tokens.size()) {
            throw new RuntimeException("Unexpected token after expression: " + engine.peek().text());
        }
        return result;
    }

    /**
     * Returns whether the given token list looks like a math expression.
     *
     * <p>A token list is considered a math expression when it contains at least one
     * arithmetic operator ({@code +}, {@code -}, {@code *}, {@code /}) or a parenthesised
     * group, and every token is either a number, an identifier, an operator, a parenthesis,
     * or a placeholder brace ({@code {}}).
     *
     * <p>Placeholder tokens ({@code {ident}}) are accepted as numeric operands when the
     * placeholder property has {@link PlaceholderType#NUMBER}.
     *
     * @param tokens the tokens to inspect
     * @param env    the type environment for placeholder type checking
     * @return {@code true} if the tokens form a math expression
     */
    public static boolean isMathExpression(@NotNull List<Token> tokens, @NotNull TypeEnv env) {
        if (tokens.size() < 3) return false;
        boolean hasOperator = false;
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            if (t.kind() == TokenKind.SYMBOL) {
                String s = t.text();
                switch (s) {
                    case "+", "-", "*", "/" -> hasOperator = true;
                    case "(", ")" -> {
                        // parentheses are fine
                    }
                    case "{" -> {
                        if (i + 2 < tokens.size()
                                && tokens.get(i + 1).kind() == TokenKind.IDENT
                                && tokens.get(i + 2).kind() == TokenKind.SYMBOL
                                && tokens.get(i + 2).text().equals("}")) {
                            String placeholder = tokens.get(i + 1).text();
                            PlaceholderType type = PlaceholderExpander.resolveType(placeholder, env);
                            if (type != PlaceholderType.NUMBER) return false;
                            i += 2;
                        } else {
                            return false;
                        }
                    }
                    default -> {
                        return false;
                    }
                }
            } else if (t.kind() == TokenKind.NUMBER) {
                // numbers are fine
            } else if (t.kind() == TokenKind.IDENT) {
                if (env.lookupVar(t.text()) == null) return false;
            } else {
                return false;
            }
        }
        return hasOperator;
    }

    private @NotNull String parseExpr() {
        StringBuilder sb = new StringBuilder();
        sb.append(parseTerm());

        while (pos < tokens.size()) {
            Token t = peek();
            if (t.kind() == TokenKind.SYMBOL && (t.text().equals("+") || t.text().equals("-"))) {
                pos++;
                sb.append(' ').append(t.text()).append(' ');
                sb.append(parseTerm());
            } else {
                break;
            }
        }

        return sb.toString();
    }

    private @NotNull String parseTerm() {
        StringBuilder sb = new StringBuilder();
        sb.append(parseFactor());

        while (pos < tokens.size()) {
            Token t = peek();
            if (t.kind() == TokenKind.SYMBOL && (t.text().equals("*") || t.text().equals("/"))) {
                pos++;
                sb.append(' ').append(t.text()).append(' ');
                sb.append(parseFactor());
            } else {
                break;
            }
        }

        return sb.toString();
    }

    private @NotNull String parseFactor() {
        if (pos >= tokens.size()) {
            throw new RuntimeException("Unexpected end of math expression");
        }

        Token t = peek();

        if (t.kind() == TokenKind.NUMBER) {
            pos++;
            return t.text();
        }

        if (t.kind() == TokenKind.IDENT) {
            pos++;
            VarRef ref = env.lookupVar(t.text());
            if (ref == null) {
                throw new RuntimeException("Variable not found in math expression: " + t.text());
            }
            return ref.java();
        }

        if (t.kind() == TokenKind.SYMBOL && t.text().equals("{")) {
            if (pos + 2 < tokens.size()
                    && tokens.get(pos + 1).kind() == TokenKind.IDENT
                    && tokens.get(pos + 2).kind() == TokenKind.SYMBOL
                    && tokens.get(pos + 2).text().equals("}")) {
                String placeholder = tokens.get(pos + 1).text();
                pos += 3;
                String java = PlaceholderExpander.resolveForExpression(placeholder, env);
                if (java == null) {
                    throw new RuntimeException("Cannot resolve placeholder in math expression: {" + placeholder + "}");
                }
                return java;
            }
        }

        if (t.kind() == TokenKind.SYMBOL && t.text().equals("(")) {
            pos++;
            String inner = parseExpr();
            if (pos >= tokens.size() || !peek().text().equals(")")) {
                throw new RuntimeException("Missing closing parenthesis in math expression");
            }
            pos++;
            return "(" + inner + ")";
        }

        throw new RuntimeException("Unexpected token in math expression: " + t.text());
    }

    private @NotNull Token peek() {
        return tokens.get(pos);
    }
}
