package dev.lumenlang.lumen.pipeline.math;

import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.placeholder.PlaceholderType;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.language.tokenization.TokenKind;
import dev.lumenlang.lumen.pipeline.placeholder.PlaceholderExpander;
import dev.lumenlang.lumen.pipeline.var.VarRef;
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
    private final int line;
    private final @NotNull String rawLine;
    private int pos;

    private MathEngine(@NotNull List<Token> tokens, @NotNull TypeEnv env, int line, @NotNull String rawLine) {
        this.tokens = tokens;
        this.env = env;
        this.line = line;
        this.rawLine = rawLine;
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
        return compile(tokens, env, 0, "");
    }

    /**
     * Compiles a list of tokens into a Java expression string.
     *
     * @param tokens  the token list representing the math expression
     * @param env     the compile-time symbol table for variable resolution
     * @param line    the script line number for diagnostic messages
     * @param rawLine the raw source text of the script line
     * @return a Java source expression string
     * @throws DiagnosticException if the expression references non-numeric operands
     * @throws RuntimeException    if the expression is malformed or references an unknown variable
     */
    public static @NotNull String compile(@NotNull List<Token> tokens, @NotNull TypeEnv env, int line, @NotNull String rawLine) {
        MathEngine engine = new MathEngine(tokens, env, line, rawLine);
        String result = engine.parseExpr();
        if (engine.pos < engine.tokens.size()) {
            throw new RuntimeException("Unexpected token after expression: " + engine.peek().text());
        }
        return result;
    }

    /**
     * Compiles a list of tokens into a Java expression string and resolves the numeric
     * result type by tracking type widening through operations.
     *
     * @param tokens  the token list representing the math expression
     * @param env     the compile-time symbol table for variable resolution
     * @param line    the script line number for diagnostic messages
     * @param rawLine the raw source text of the script line
     * @return a typed result containing the Java expression and its resolved type
     * @throws DiagnosticException if the expression references non-numeric operands
     * @throws RuntimeException    if the expression is malformed or references an unknown variable
     */
    public static @NotNull TypedResult compileTyped(@NotNull List<Token> tokens, @NotNull TypeEnv env, int line, @NotNull String rawLine) {
        MathEngine engine = new MathEngine(tokens, env, line, rawLine);
        TypedFragment fragment = engine.parseExprTyped();
        if (engine.pos < engine.tokens.size()) {
            throw new RuntimeException("Unexpected token after expression: " + engine.peek().text());
        }
        return new TypedResult(fragment.java, fragment.type);
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
            } else if (t.kind() == TokenKind.STRING) {
                // strings are accepted so MathEngine can produce a proper type error
            } else if (t.kind() == TokenKind.IDENT) {
                if (env.lookupVar(t.text()) == null) return false;
            } else {
                return false;
            }
        }
        return hasOperator;
    }

    private static @NotNull LumenType widenTypes(@NotNull LumenType a, @NotNull LumenType b) {
        PrimitiveType widened = LumenType.widenNumeric(a, b);
        return widened != null ? widened : PrimitiveType.INT;
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

        if (t.kind() == TokenKind.STRING) {
            pos++;
            throw new DiagnosticException(LumenDiagnostic.error("E203", "Non-numeric operand in arithmetic expression").at(line, rawLine).highlight(t.start(), t.end()).label("string literal is not numeric").help("use 'combined string of x and y' to concatenate strings").build());
        }

        if (t.kind() == TokenKind.IDENT) {
            pos++;
            VarRef ref = env.lookupVar(t.text());
            if (ref == null) {
                throw new RuntimeException("Variable not found in math expression: " + t.text());
            }
            LumenType type = ref.type();
            if (!type.numeric()) {
                LumenDiagnostic.Builder b = LumenDiagnostic.error("E203", "Non-numeric operand in arithmetic expression").at(line, rawLine).highlight(t.start(), t.end()).label("'" + t.text() + "' is '" + type.displayName() + "', not numeric");
                if (type.unwrap() == PrimitiveType.STRING) {
                    b.help("use 'combined string of x and y' to concatenate strings");
                }
                throw new DiagnosticException(b.build());
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

    private @NotNull TypedFragment parseExprTyped() {
        TypedFragment left = parseTermTyped();

        while (pos < tokens.size()) {
            Token t = peek();
            if (t.kind() == TokenKind.SYMBOL && (t.text().equals("+") || t.text().equals("-"))) {
                pos++;
                TypedFragment right = parseTermTyped();
                LumenType widened = widenTypes(left.type, right.type);
                left = new TypedFragment(left.java + " " + t.text() + " " + right.java, widened);
            } else {
                break;
            }
        }

        return left;
    }

    private @NotNull TypedFragment parseTermTyped() {
        TypedFragment left = parseFactorTyped();

        while (pos < tokens.size()) {
            Token t = peek();
            if (t.kind() == TokenKind.SYMBOL && (t.text().equals("*") || t.text().equals("/"))) {
                pos++;
                TypedFragment right = parseFactorTyped();
                LumenType widened = widenTypes(left.type, right.type);
                if (t.text().equals("/") && widened == PrimitiveType.INT) {
                    widened = PrimitiveType.DOUBLE;
                }
                left = new TypedFragment(left.java + " " + t.text() + " " + right.java, widened);
            } else {
                break;
            }
        }

        return left;
    }

    private @NotNull TypedFragment parseFactorTyped() {
        if (pos >= tokens.size()) {
            throw new RuntimeException("Unexpected end of math expression");
        }

        Token t = peek();

        if (t.kind() == TokenKind.NUMBER) {
            pos++;
            LumenType type = t.text().contains(".") ? PrimitiveType.DOUBLE : PrimitiveType.INT;
            return new TypedFragment(t.text(), type);
        }

        if (t.kind() == TokenKind.STRING) {
            pos++;
            throw new DiagnosticException(LumenDiagnostic.error("E203", "Non-numeric operand in arithmetic expression").at(line, rawLine).highlight(t.start(), t.end()).label("string literal is not numeric").help("use 'combined string of x and y' to concatenate strings").build());
        }

        if (t.kind() == TokenKind.IDENT) {
            pos++;
            VarRef ref = env.lookupVar(t.text());
            if (ref == null) {
                throw new RuntimeException("Variable not found in math expression: " + t.text());
            }
            LumenType type = ref.type();
            if (!type.numeric()) {
                LumenDiagnostic.Builder b = LumenDiagnostic.error("E203", "Non-numeric operand in arithmetic expression").at(line, rawLine).highlight(t.start(), t.end()).label("'" + t.text() + "' is '" + type.displayName() + "', not numeric");
                if (type.unwrap() == PrimitiveType.STRING) {
                    b.help("use 'combined string of x and y' to concatenate strings");
                }
                throw new DiagnosticException(b.build());
            }
            return new TypedFragment(ref.java(), type);
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
                LumenType phType = PlaceholderExpander.resolveExpressionType(placeholder, env);
                return new TypedFragment(java, phType);
            }
        }

        if (t.kind() == TokenKind.SYMBOL && t.text().equals("(")) {
            pos++;
            TypedFragment inner = parseExprTyped();
            if (pos >= tokens.size() || !peek().text().equals(")")) {
                throw new RuntimeException("Missing closing parenthesis in math expression");
            }
            pos++;
            return new TypedFragment("(" + inner.java + ")", inner.type);
        }

        throw new RuntimeException("Unexpected token in math expression: " + t.text());
    }

    private @NotNull Token peek() {
        return tokens.get(pos);
    }

    /**
     * The result of a typed math compilation, carrying both the Java source and the
     * resolved numeric type.
     *
     * @param java the Java expression source string
     * @param type the resolved numeric type
     */
    public record TypedResult(@NotNull String java, @NotNull LumenType type) {
    }

    private record TypedFragment(@NotNull String java, @NotNull LumenType type) {
    }
}
