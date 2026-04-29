package dev.lumenlang.lumen.pipeline.language.typed;

import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.language.nodes.StatementNode;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredExpressionMatch;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredPatternMatch;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Classifies a raw {@link StatementNode} into a specific {@link TypedStatement} variant.
 *
 * <p>Classifies statement nodes into typed variants using pattern matching, expression
 * matching, and fuzzy suggestions.
 *
 * @see TypedStatement
 */
public final class StatementClassifier {

    /**
     * Classifies the given statement node into a typed statement variant.
     *
     * @param st  the raw statement node to classify
     * @param reg the pattern registry for matching statements and expressions
     * @param env the type environment for variable lookups and scoping
     * @return the classified typed statement (never null)
     */
    public static @NotNull TypedStatement classify(
            @NotNull StatementNode st,
            @NotNull PatternRegistry reg,
            @NotNull TypeEnv env) {
        List<Token> t = st.head();

        RegisteredPatternMatch m = reg.matchStatement(t, env);
        if (m != null)
            return new TypedStatement.PatternStmt(st, m);

        RegisteredExpressionMatch exprFallback = reg.matchExpression(t, env);
        if (exprFallback != null)
            return new TypedStatement.ExprStmt(st, exprFallback);

        RegisteredPatternMatch slowM = reg.matchStatementSlow(t, env);
        if (slowM != null)
            return new TypedStatement.PatternStmt(st, slowM);

        RegisteredExpressionMatch slowExpr = reg.matchExpressionSlow(t, env);
        if (slowExpr != null)
            return new TypedStatement.ExprStmt(st, slowExpr);

        return new TypedStatement.ErrorStmt(
                st,
                "Unknown statement on line " + st.line() + ". No matching statement or expression pattern was found",
                t,
                PatternSimulator.suggestStatementsAndExpressions(t, reg, env));
    }
}
