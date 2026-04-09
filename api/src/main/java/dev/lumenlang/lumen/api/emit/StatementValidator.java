package dev.lumenlang.lumen.api.emit;

import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Validates a statement's tokens before pattern matching and code generation.
 *
 * <p>Registered validators run after all statement form handlers have been tried (and none
 * handled the statement) but before pattern matching. This is the appropriate place for
 * cross-cutting concerns like null safety checks, type flow analysis, or custom lint rules.
 *
 * <p>Addons can register their own validators to enforce project-specific rules.
 *
 * @see EmitRegistrar#statementValidator(StatementValidator)
 */
@FunctionalInterface
public interface StatementValidator {

    /**
     * Validates the given statement tokens.
     *
     * <p>If the statement is invalid, the implementation should throw a {@link DiagnosticException} with a descriptive diagnostic.
     *
     * @param tokens the statement tokens to validate
     * @param ctx    the emit context providing environment, codegen, and output access
     */
    void validate(@NotNull List<? extends ScriptToken> tokens, @NotNull EmitContext ctx);
}
