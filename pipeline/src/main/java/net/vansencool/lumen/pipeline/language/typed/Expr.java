package net.vansencool.lumen.pipeline.language.typed;

import net.vansencool.lumen.pipeline.language.tokenization.Token;
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
 * @see ExprParser
 * @see TypedStatement.VarStmt
 */
public sealed interface Expr permits
        Expr.Literal,
        Expr.RefExpr,
        Expr.MathExpr,
        Expr.RawExpr {

    /**
     * A constant literal value such as a number or a quoted string.
     *
     * <p>
     * The value is stored as its native Java type (e.g. {@link Long},
     * {@link Double},
     * {@link String}) so the code generator can emit the appropriate literal
     * syntax.
     *
     * @param value the parsed constant value, or {@code null} for an explicitly
     *              null literal
     */
    record Literal(@Nullable Object value) implements Expr {
    }

    /**
     * A reference to a previously declared variable by name.
     *
     * <p>
     * During code generation this is emitted as the Java variable name that was
     * assigned
     * when the variable was first declared in the compiled class.
     *
     * @param name the variable name as it appears in the script
     */
    record RefExpr(@NotNull String name) implements Expr {
    }

    /**
     * A compiled math expression that has already been converted to a Java source
     * string.
     *
     * @param java the Java expression source code
     */
    record MathExpr(@NotNull String java) implements Expr {
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
     * @param tokens the unprocessed tokens forming this expression
     */
    record RawExpr(@NotNull List<Token> tokens) implements Expr {
    }
}
