package dev.lumenlang.lumen.pipeline.language.typed;

import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.language.nodes.StatementNode;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredPatternMatch;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Classifies a raw {@link StatementNode} into a specific {@link TypedStatement} variant.
 */
public final class StatementClassifier {

    public static @NotNull TypedStatement classify(@NotNull StatementNode st, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env) {
        List<Token> t = st.head();
        RegisteredPatternMatch m = reg.matchStatement(t, env);
        if (m != null) return new TypedStatement.PatternStmt(st, m);
        RegisteredPatternMatch slowM = reg.matchStatementSlow(t, env);
        if (slowM != null) return new TypedStatement.PatternStmt(st, slowM);
        return new TypedStatement.ErrorStmt(st, "Unknown statement on line " + st.line(), t, PatternSimulator.suggestStatements(t, reg, env));
    }
}
