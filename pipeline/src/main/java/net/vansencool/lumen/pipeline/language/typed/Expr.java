package net.vansencool.lumen.pipeline.language.typed;

import net.vansencool.lumen.pipeline.language.tokenization.Token;
import net.vansencool.lumen.pipeline.type.LumenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a parsed expression within a Lumen script.
 *
 * <p>
 * Expressions are produced by the {@link ExprParser} when it classifies the
 * right-hand side
 * of a variable assignment or a standalone value. Each variant captures a
 * different kind of
 * expression that the code generator handles independently.
 *
 * <p>Every variant carries an optional {@link LumenType} that describes the
 * compile-time type of the expression, when it can be determined.
 *
 * @see ExprParser
 * @see TypedStatement.VarStmt
 */
public sealed interface Expr permits
        Expr.Literal,
        Expr.RefExpr,
        Expr.MathExpr,
        Expr.RawExpr {

    /**
     * Returns the compile-time type of this expression, or {@code null} if unknown.
     *
     * @return the resolved type, or {@code null}
     */
    @Nullable LumenType resolvedType();

    /**
     * A constant literal value such as a number or a quoted string.
     *
     * <p>
     * The value is stored as its native Java type (e.g. {@link Long},
     * {@link Double},
     * {@link String}) so the code generator can emit the appropriate literal
     * syntax.
     *
     * @param value        the parsed constant value, or {@code null} for an explicitly
     *                     null literal
     * @param resolvedType the compile-time type inferred from the literal value
     */
    record Literal(@Nullable Object value, @Nullable LumenType resolvedType) implements Expr {

        /**
         * Creates a literal with type inferred from the value.
         *
         * @param value the literal value
         */
        public Literal(@Nullable Object value) {
            this(value, inferType(value));
        }

        private static @Nullable LumenType inferType(@Nullable Object value) {
            if (value instanceof Integer) return LumenType.Primitive.INT;
            if (value instanceof Long) return LumenType.Primitive.LONG;
            if (value instanceof Double) return LumenType.Primitive.DOUBLE;
            if (value instanceof Float) return LumenType.Primitive.FLOAT;
            if (value instanceof Boolean) return LumenType.Primitive.BOOLEAN;
            if (value instanceof String) return LumenType.Primitive.STRING;
            return null;
        }
    }

    /**
     * A reference to a previously declared variable by name.
     *
     * <p>
     * During code generation this is emitted as the Java variable name that was
     * assigned
     * when the variable was first declared in the compiled class.
     *
     * @param name         the variable name as it appears in the script
     * @param resolvedType the compile-time type from the variable's definition
     */
    record RefExpr(@NotNull String name, @Nullable LumenType resolvedType) implements Expr {

        /**
         * Creates a ref expression without type information.
         *
         * @param name the variable name
         */
        public RefExpr(@NotNull String name) {
            this(name, null);
        }
    }

    /**
     * A compiled math expression that has already been converted to a Java source
     * string.
     *
     * @param java         the Java expression source code
     * @param resolvedType the compile-time numeric result type
     */
    record MathExpr(@NotNull String java, @Nullable LumenType resolvedType) implements Expr {

        /**
         * Creates a math expression without type information.
         *
         * @param java the Java expression source code
         */
        public MathExpr(@NotNull String java) {
            this(java, null);
        }
    }

    /**
     * A raw, unparsed expression represented as a list of tokens.
     *
     * <p>
     * This is used as a fallback when the expression does not match any other
     * variant.
     * The code generator handles raw expressions by delegating to the type binding
     * system.
     *
     * @param tokens       the unprocessed tokens forming this expression
     * @param resolvedType the compile-time type if it could be determined, or {@code null}
     */
    record RawExpr(@NotNull List<Token> tokens, @Nullable LumenType resolvedType) implements Expr {

        /**
         * Creates a raw expression without type information.
         *
         * @param tokens the token list
         */
        public RawExpr(@NotNull List<Token> tokens) {
            this(tokens, null);
        }
    }
}
