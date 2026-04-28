package dev.lumenlang.lumen.pipeline.type;

import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.NullableType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Static type checking utilities for the Lumen type system.
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

        if (sourceType instanceof NullableType && !(targetType instanceof NullableType)) {
            return LumenDiagnostic.error("Cannot assign nullable value to non-nullable variable")
                    .at(line, sourceText).highlight(colStart, colEnd)
                    .label("expected '" + targetType.displayName() + "', found '" + sourceType.displayName() + "'")
                    .note("variable '" + varName + "' is declared as '" + targetType.displayName() + "' which cannot hold 'none'")
                    .help("declare as 'nullable " + targetType.displayName() + "' to allow 'none'")
                    .build();
        }

        if (targetType.numeric() && sourceType.numeric()) {
            return LumenDiagnostic.error("Lossy numeric conversion")
                    .at(line, sourceText).highlight(colStart, colEnd)
                    .label("expected '" + targetType.displayName() + "', found '" + sourceType.displayName() + "'")
                    .note("converting '" + sourceType.displayName() + "' to '" + targetType.displayName() + "' may lose precision")
                    .help("declare the variable as '" + sourceType.displayName() + "' instead, or use an explicit conversion")
                    .build();
        }

        return LumenDiagnostic.error("Type mismatch in assignment")
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
        if (targetType instanceof NullableType) return null;

        return LumenDiagnostic.error("Cannot assign 'none' to non-nullable variable")
                .at(line, sourceText).highlight(colStart, colEnd)
                .label("'" + varName + "' is not nullable")
                .note("variable '" + varName + "' has type '" + targetType.displayName() + "' which does not accept 'none'")
                .help("declare as 'nullable " + targetType.displayName() + "' to allow 'none'")
                .build();
    }

    /**
     * Validates that a nullable variable is being accessed safely, with full value flow context.
     *
     * @param type            the variable type
     * @param varName         the variable name
     * @param isNullChecked   whether the variable has been verified non-null in this scope
     * @param line            the script line number of the usage
     * @param sourceText      the raw script line of the usage
     * @param colStart        the start column of the usage
     * @param colEnd          the end column of the usage
     * @param declLine        the line where the variable was declared, or {@code -1} if unknown
     * @param declSource      the raw source text of the declaration line, or {@code null} if unknown
     * @param nullAssignLine  the line where the variable was last set to none, or {@code -1} if same as declaration
     * @param nullAssignSource the raw source text of the null assignment line, or {@code null}
     * @return a diagnostic if the access is unsafe, or {@code null} if safe
     */
    public static @Nullable LumenDiagnostic checkNullSafety(@NotNull LumenType type, @NotNull String varName, boolean isNullChecked, int line, @NotNull String sourceText, int colStart, int colEnd, int declLine, @Nullable String declSource, int nullAssignLine, @Nullable String nullAssignSource) {
        if (!(type instanceof NullableType)) return null;
        if (isNullChecked) return null;

        LumenDiagnostic.Builder b = LumenDiagnostic.error("Variable '" + varName + "' is none")
                .at(line, sourceText).highlight(colStart, colEnd)
                .label("'" + varName + "' is 'none' here");
        if (nullAssignLine > 0 && nullAssignSource != null && nullAssignLine != declLine) {
            b.context(nullAssignLine, nullAssignSource, 0, nullAssignSource.stripTrailing().length(), "set to 'none' here");
            b.note("'" + varName + "' was set to 'none' on line " + nullAssignLine);
        } else if (declLine > 0 && declSource != null) {
            b.context(declLine, declSource, 0, declSource.stripTrailing().length(), "declared without a value here");
            b.note("'" + varName + "' was declared without a default value on line " + declLine);
        } else {
            b.note("'" + varName + "' has type '" + type.displayName() + "' and no value was assigned");
        }
        b.help("provide a default value, or check with 'if " + varName + " is not none:' before using");
        return b.build();
    }
}
