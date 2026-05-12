package dev.lumenlang.lumen.pipeline.language.typed;

import dev.lumenlang.lumen.pipeline.language.nodes.StatementNode;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredPatternMatch;
import dev.lumenlang.lumen.pipeline.language.simulator.result.Suggestion;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Typed statement classified for code generation.
 */
public sealed interface TypedStatement permits TypedStatement.PatternStmt, TypedStatement.ErrorStmt {

    StatementNode raw();

    /**
     * Matched against a registered statement pattern.
     */
    record PatternStmt(StatementNode raw, RegisteredPatternMatch match) implements TypedStatement {
    }

    /**
     * No pattern matched; carries suggestor output for the diagnostic.
     */
    record ErrorStmt(@NotNull StatementNode raw, @NotNull String message, @Nullable List<Token> errorTokens, @NotNull List<Suggestion> suggestions) implements TypedStatement {
    }
}
