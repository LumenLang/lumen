package dev.lumenlang.lumen.pipeline.type;

import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Static type checking utilities for the Lumen type system.
 *
 * <p>Each method validates a specific class of type constraint and produces a rich
 * {@link LumenDiagnostic} when the constraint is violated. These diagnostics include
 * the exact source location, expected and actual types, and actionable suggestions.
 */
public final class TypeChecker {

    private TypeChecker() {
    }

    /**
     * Validates that a value of {@code sourceType} can be assigned to a variable of {@code targetType}.
     *
     * @param targetType the declared type of the variable
     * @param sourceType the type of the value being assigned
     * @param varName    the variable name (for error messages)
     * @param line       the script line number
     * @param sourceText the raw script line
     * @param colStart   the start column of the expression
     * @param colEnd     the end column of the expression
     * @return a diagnostic if the assignment is invalid, or {@code null} if it is valid
     */
    public static @Nullable LumenDiagnostic checkAssignment(@NotNull LumenType targetType, @NotNull LumenType sourceType, @NotNull String varName, int line, @NotNull String sourceText, int colStart, int colEnd) {
        if (targetType.assignableFrom(sourceType)) return null;

        if (sourceType instanceof LumenType.NullableType && !(targetType instanceof LumenType.NullableType)) {
            return LumenDiagnostic.error("E102", "Cannot assign nullable value to non-nullable variable")
                    .at(line, sourceText).highlight(colStart, colEnd)
                    .label("expected '" + targetType.displayName() + "', found '" + sourceType.displayName() + "'")
                    .note("variable '" + varName + "' is declared as '" + targetType.displayName() + "' which cannot hold 'none'")
                    .help("declare as 'nullable " + targetType.displayName() + "' to allow 'none'")
                    .build();
        }

        if (targetType.numeric() && sourceType.numeric()) {
            return LumenDiagnostic.error("E103", "Lossy numeric conversion")
                    .at(line, sourceText).highlight(colStart, colEnd)
                    .label("expected '" + targetType.displayName() + "', found '" + sourceType.displayName() + "'")
                    .note("converting '" + sourceType.displayName() + "' to '" + targetType.displayName() + "' may lose precision")
                    .help("declare the variable as '" + sourceType.displayName() + "' instead, or use an explicit conversion")
                    .build();
        }

        return LumenDiagnostic.error("E100", "Type mismatch in assignment")
                .at(line, sourceText).highlight(colStart, colEnd)
                .label("expected '" + targetType.displayName() + "', found '" + sourceType.displayName() + "'")
                .note("variable '" + varName + "' was declared as '" + targetType.displayName() + "'")
                .help("once declared, a variable's type cannot change")
                .build();
    }

    /**
     * Validates that 'none' can be assigned to a variable of the given type.
     *
     * @param targetType the declared type of the variable
     * @param varName    the variable name
     * @param line       the script line number
     * @param sourceText the raw script line
     * @param colStart   the start column of 'none'
     * @param colEnd     the end column of 'none'
     * @return a diagnostic if the target is non-nullable, or {@code null} if valid
     */
    public static @Nullable LumenDiagnostic checkNullAssignment(@NotNull LumenType targetType, @NotNull String varName, int line, @NotNull String sourceText, int colStart, int colEnd) {
        if (targetType instanceof LumenType.NullableType) return null;

        return LumenDiagnostic.error("E101", "Cannot assign 'none' to non-nullable variable")
                .at(line, sourceText).highlight(colStart, colEnd)
                .label("'" + varName + "' is not nullable")
                .note("variable '" + varName + "' has type '" + targetType.displayName() + "' which does not accept 'none'")
                .help("declare as 'nullable " + targetType.displayName() + "' to allow 'none'")
                .build();
    }

    /**
     * Validates that both operands of a binary arithmetic operator have compatible numeric types.
     *
     * @param op         the operator symbol (e.g. {@code "+"}, {@code "-"})
     * @param leftType   the left operand type
     * @param rightType  the right operand type
     * @param line       the script line number
     * @param sourceText the raw script line
     * @param colStart   the start column of the expression
     * @param colEnd     the end column of the expression
     * @return a diagnostic if the operands are incompatible, or {@code null} if valid
     */
    public static @Nullable LumenDiagnostic checkArithmetic(@NotNull String op, @NotNull LumenType leftType, @NotNull LumenType rightType, int line, @NotNull String sourceText, int colStart, int colEnd) {
        LumenType left = leftType.unwrap();
        LumenType right = rightType.unwrap();

        if (!left.numeric() || !right.numeric()) {
            String label;
            if (!left.numeric() && !right.numeric()) {
                label = "left is '" + left.displayName() + "', right is '" + right.displayName() + "'";
            } else if (!left.numeric()) {
                label = "left operand '" + left.displayName() + "' is not numeric";
            } else {
                label = "right operand '" + right.displayName() + "' is not numeric";
            }
            LumenDiagnostic.Builder b = LumenDiagnostic.error("E201", "Invalid operands for '" + op + "'")
                    .at(line, sourceText).highlight(colStart, colEnd).label(label);
            if (left == LumenType.Primitive.STRING || right == LumenType.Primitive.STRING) {
                b.help("use 'combined string of x and y' to concatenate strings");
            }
            return b.build();
        }

        return null;
    }

    /**
     * Validates that a comparison operator is applied to compatible types.
     *
     * @param leftType   the left operand type
     * @param rightType  the right operand type
     * @param line       the script line number
     * @param sourceText the raw script line
     * @param colStart   the start column
     * @param colEnd     the end column
     * @return a diagnostic if the types are incompatible, or {@code null} if valid
     */
    public static @Nullable LumenDiagnostic checkComparison(@NotNull LumenType leftType, @NotNull LumenType rightType, int line, @NotNull String sourceText, int colStart, int colEnd) {
        LumenType left = leftType.unwrap();
        LumenType right = rightType.unwrap();
        if (left.equals(right)) return null;
        if (left.numeric() && right.numeric()) return null;

        return LumenDiagnostic.error("E202", "Cannot compare values of different types")
                .at(line, sourceText).highlight(colStart, colEnd)
                .label("expected '" + leftType.displayName() + "', found '" + rightType.displayName() + "'")
                .build();
    }

    /**
     * Validates that a nullable variable is being accessed safely (after a null check).
     *
     * @param type          the variable type
     * @param varName       the variable name
     * @param isNullChecked whether the variable has been verified non-null in this scope
     * @param line          the script line number
     * @param sourceText    the raw script line
     * @param colStart      the start column
     * @param colEnd        the end column
     * @return a diagnostic if the access is unsafe, or {@code null} if safe
     */
    public static @Nullable LumenDiagnostic checkNullSafety(@NotNull LumenType type, @NotNull String varName, boolean isNullChecked, int line, @NotNull String sourceText, int colStart, int colEnd) {
        return checkNullSafety(type, varName, isNullChecked, line, sourceText, colStart, colEnd, -1, null);
    }

    /**
     * Validates that a nullable variable is being accessed safely, with optional declaration context for multi-line diagnostics.
     *
     * @param type          the variable type
     * @param varName       the variable name
     * @param isNullChecked whether the variable has been verified non-null in this scope
     * @param line          the script line number of the usage
     * @param sourceText    the raw script line of the usage
     * @param colStart      the start column of the usage
     * @param colEnd        the end column of the usage
     * @param declLine      the line where the variable was declared, or {@code -1} if unknown
     * @param declSource    the raw source text of the declaration line, or {@code null} if unknown
     * @return a diagnostic if the access is unsafe, or {@code null} if safe
     */
    public static @Nullable LumenDiagnostic checkNullSafety(@NotNull LumenType type, @NotNull String varName, boolean isNullChecked, int line, @NotNull String sourceText, int colStart, int colEnd, int declLine, @Nullable String declSource) {
        if (!(type instanceof LumenType.NullableType)) return null;
        if (isNullChecked) return null;

        LumenDiagnostic.Builder b = LumenDiagnostic.error("E301", "Variable '" + varName + "' is none")
                .at(line, sourceText).highlight(colStart, colEnd)
                .label("'" + varName + "' is 'none' here");
        if (declLine > 0 && declSource != null) {
            b.context(declLine, declSource, 0, declSource.stripTrailing().length(), "declared as nullable here");
        }
        b.note("'" + varName + "' has type '" + type.displayName() + "' and no value was assigned")
                .help("provide a default value like 'set " + varName + " to nullable " + ((LumenType.NullableType) type).inner().displayName() + " \"...\"', or reassign to a non-null value before using");
        return b.build();
    }

    /**
     * Validates that an element being added to a collection matches the collection's element type.
     *
     * @param collectionType the collection type
     * @param elementType    the type of the element being added
     * @param line           the script line number
     * @param sourceText     the raw script line
     * @param colStart       the start column
     * @param colEnd         the end column
     * @return a diagnostic if the element type is incompatible, or {@code null} if valid
     */
    public static @Nullable LumenDiagnostic checkCollectionElement(@NotNull LumenType.CollectionType collectionType, @NotNull LumenType elementType, int line, @NotNull String sourceText, int colStart, int colEnd) {
        if (collectionType.elementType().assignableFrom(elementType)) return null;

        return LumenDiagnostic.error("E401", "Collection element type mismatch")
                .at(line, sourceText).highlight(colStart, colEnd)
                .label("expected '" + collectionType.elementType().displayName() + "', found '" + elementType.displayName() + "'")
                .note("this " + collectionType.displayName() + " only accepts '" + collectionType.elementType().displayName() + "' elements")
                .build();
    }

    /**
     * Validates that a variable used in a math expression is numeric.
     *
     * @param type       the variable type
     * @param varName    the variable name
     * @param line       the script line number
     * @param sourceText the raw script line
     * @param colStart   the start column
     * @param colEnd     the end column
     * @return a diagnostic if the type is non-numeric, or {@code null} if numeric
     */
    public static @Nullable LumenDiagnostic checkNumericOperand(@NotNull LumenType type, @NotNull String varName, int line, @NotNull String sourceText, int colStart, int colEnd) {
        if (type.unwrap().numeric()) return null;

        LumenDiagnostic.Builder b = LumenDiagnostic.error("E203", "Non-numeric operand in arithmetic expression")
                .at(line, sourceText).highlight(colStart, colEnd)
                .label("'" + varName + "' is '" + type.displayName() + "', not numeric");
        if (type.unwrap() == LumenType.Primitive.STRING) {
            b.help("use 'combined string of x and y' to concatenate strings instead of '+'");
        }
        return b.build();
    }

    /**
     * Creates a diagnostic for declaring a variable without type information.
     *
     * @param varName    the variable name
     * @param exprText   the expression text that could not be typed
     * @param line       the script line number
     * @param sourceText the raw script line
     * @param colStart   the start column
     * @param colEnd     the end column
     * @return the diagnostic
     */
    public static @NotNull LumenDiagnostic unresolvableType(@NotNull String varName, @NotNull String exprText, int line, @NotNull String sourceText, int colStart, int colEnd) {
        return LumenDiagnostic.error("E501", "Cannot determine type of expression")
                .at(line, sourceText).highlight(colStart, colEnd)
                .label("type unknown")
                .note("expression '" + exprText + "' does not have a known type")
                .help("ensure all expressions return a typed result")
                .build();
    }
}
