package dev.lumenlang.lumen.pipeline.language.typed;

import dev.lumenlang.lumen.api.emit.StatementFormHandler;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.language.nodes.StatementNode;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredExpressionMatch;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredPatternMatch;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Classifies a raw {@link StatementNode} into a specific {@link TypedStatement} variant.
 *
 * <p>Operates as a fallback after all registered {@link StatementFormHandler} implementations
 * have been tried. Variable declarations ({@code set}, {@code global}, {@code load}) are
 * handled by form handlers and are not classified here except as a fallback for expression
 * patterns.
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

        if (isLocalVarDeclaration(t, env)) {
            String name = t.get(1).text();
            List<Token> exprTokens = t.subList(3, t.size());

            RegisteredExpressionMatch exprMatch = reg.matchExpressionSlow(exprTokens, env);
            if (exprMatch == null) {
                exprMatch = reg.matchExpression(exprTokens, env);
            }
            if (exprMatch != null) {
                return new TypedStatement.ExprVarStmt(st, name, exprMatch);
            }

            Expr expr = ExprParser.parse(exprTokens, env);
            return new TypedStatement.VarStmt(st, name, expr);
        }

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
                t);
    }

    private static boolean isLocalVarDeclaration(@NotNull List<Token> t, @NotNull TypeEnv env) {
        if (t.size() < 4) return false;
        String name = t.get(1).text();
        return t.get(0).text().equalsIgnoreCase("set")
                && t.get(2).text().equalsIgnoreCase("to")
                && env.lookupVar(name) == null
                && !env.isGlobalField(name)
                && env.getGlobalInfo(name) == null;
    }
}
