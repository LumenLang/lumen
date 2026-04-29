package dev.lumenlang.lumen.pipeline.language.typed;

import dev.lumenlang.lumen.pipeline.language.nodes.StatementNode;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredExpressionMatch;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredPatternMatch;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A typed statement that has been classified into a specific category (e.g. pattern match, expression, etc.)
 * and carries the relevant parsed information for code generation.
 */
public sealed interface TypedStatement permits TypedStatement.PatternStmt, TypedStatement.ExprStmt, TypedStatement.ErrorStmt {

    StatementNode raw();

    /**
     * A statement that matched a registered pattern.
     *
     * <p>
     * Carries the full {@link RegisteredPatternMatch} - both the bound parameter
     * values
     * and the handler reference - so that the emitter does not need to re-run
     * pattern matching
     * when it is time to generate code.
     *
     * @param raw   the original statement node
     * @param match the complete match result including handler reference
     */
    record PatternStmt(StatementNode raw, RegisteredPatternMatch match) implements TypedStatement {
    }

    /**
     * A standalone expression used as a statement.
     *
     * <p>
     * The expression result is evaluated for its side effects (e.g. spawning an
     * entity)
     * and the return value is discarded.
     *
     * @param raw   the original statement node
     * @param match the expression pattern match
     */
    record ExprStmt(StatementNode raw, RegisteredExpressionMatch match) implements TypedStatement {
    }

    /**
     * A statement that could not be classified and carries a diagnostic error
     * message.
     *
     * <p>
     * Error statements are produced when a line does not match any known pattern,
     * variable declaration, or expression. The {@code errorTokens} field, when
     * present,
     * identifies the specific tokens that caused the failure so that the error
     * reporter
     * can render a squiggly underline beneath them.
     *
     * @param raw         the original statement node
     * @param message     the human-readable error description
     * @param errorTokens the tokens responsible for the error, or {@code null} if the entire line is at fault
     * @param suggestions simulation results from the pattern simulator, empty if no close matches were found
     */
    record ErrorStmt(@NotNull StatementNode raw, @NotNull String message, @Nullable List<Token> errorTokens, @NotNull List<PatternSimulator.Suggestion> suggestions) implements TypedStatement {
    }
}
