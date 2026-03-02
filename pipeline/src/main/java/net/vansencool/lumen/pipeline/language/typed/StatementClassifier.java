package net.vansencool.lumen.pipeline.language.typed;

import net.vansencool.lumen.pipeline.codegen.TypeEnv;
import net.vansencool.lumen.pipeline.language.nodes.StatementNode;
import net.vansencool.lumen.pipeline.language.pattern.PatternRegistry;
import net.vansencool.lumen.pipeline.language.pattern.RegisteredExpressionMatch;
import net.vansencool.lumen.pipeline.language.pattern.RegisteredPatternMatch;
import net.vansencool.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Classifies a raw {@link StatementNode} into a specific {@link TypedStatement}
 * variant.
 *
 * <p>
 * Classification is attempted in the following order:
 * <ol>
 * <li>{@code global [stored] var x [for [ref type] refType] default expr} &rarr;
 * {@link TypedStatement.GlobalVarStmt}</li>
 * <li>{@code global x [for refType] default expr} (legacy) &rarr;
 * {@link TypedStatement.GlobalVarStmt}</li>
 * <li>{@code stored var x [for [ref type] scope] default expr} &rarr;
 * {@link TypedStatement.StoreVarStmt}</li>
 * <li>{@code store x [for scope] default expr} (legacy) &rarr;
 * {@link TypedStatement.StoreVarStmt}</li>
 * <li>{@code var x = expr} &rarr; {@link TypedStatement.ExprVarStmt} or
 * {@link TypedStatement.VarStmt}</li>
 * <li>Registered statement pattern match &rarr;
 * {@link TypedStatement.PatternStmt}</li>
 * <li>Registered expression pattern match &rarr;
 * {@link TypedStatement.ExprStmt}</li>
 * <li>Fallback &rarr; {@link TypedStatement.ErrorStmt}</li>
 * </ol>
 *
 * @see TypedStatement
 * @see ExprParser
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

        if (isNewGlobalStatement(t)) {
            return classifyNewGlobal(st, t, reg, env);
        }

        if (isGlobalStatement(t)) {
            String name = t.get(1).text();
            String refTypeName = null;
            int defaultIdx;

            if (t.get(2).text().equalsIgnoreCase("for")) {
                refTypeName = t.get(3).text();
                defaultIdx = 4;
            } else {
                defaultIdx = 2;
            }

            List<Token> exprTokens = t.subList(defaultIdx + 1, t.size());
            RegisteredExpressionMatch globalExprMatch = reg.matchExpressionSlow(exprTokens, env);
            if (globalExprMatch == null) {
                globalExprMatch = reg.matchExpression(exprTokens, env);
            }
            Expr expr = ExprParser.parse(exprTokens, env);
            return new TypedStatement.GlobalVarStmt(st, name, refTypeName, expr, globalExprMatch, true);
        }

        if (isStoredVarStatement(t)) {
            return classifyStoredVar(st, t, env);
        }

        if (isStoreStatement(t)) {
            String name = t.get(1).text();
            String scopeVar = null;
            int defaultIdx;

            if (t.get(2).text().equalsIgnoreCase("for")) {
                scopeVar = t.get(3).text();
                defaultIdx = 4;
            } else {
                defaultIdx = 2;
            }

            List<Token> exprTokens = t.subList(defaultIdx + 1, t.size());
            Expr expr = ExprParser.parse(exprTokens, env);
            return new TypedStatement.StoreVarStmt(st, name, scopeVar, expr);
        }

        if (isVarStatement(t)) {
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

        String errorMessage = buildUnknownStatementMessage(t, st.line());
        return new TypedStatement.ErrorStmt(
                st,
                errorMessage,
                t);
    }

    /**
     * Builds a descriptive error message for an unrecognized statement.
     *
     * <p>Detects common mistakes like malformed variable declarations and provides
     * targeted suggestions.
     *
     * @param t    the token list of the statement
     * @param line the source line number
     * @return a descriptive error message
     */
    private static @NotNull String buildUnknownStatementMessage(@NotNull List<Token> t, int line) {
        if (!t.isEmpty() && t.get(0).text().equalsIgnoreCase("var")) {
            if (t.size() == 1) {
                return "Incomplete variable declaration on line " + line
                        + ". Expected: var <name> = <value>";
            }

            String attemptedName = t.get(1).text();

            if (t.size() >= 3 && !t.get(2).text().equals("=")) {
                boolean hasEqualsLater = false;
                for (int i = 3; i < t.size(); i++) {
                    if (t.get(i).text().equals("=")) {
                        hasEqualsLater = true;
                        break;
                    }
                }

                if (hasEqualsLater) {
                    StringBuilder fullName = new StringBuilder(attemptedName);
                    for (int i = 2; !t.get(i).text().equals("="); i++) {
                        fullName.append(t.get(i).text());
                    }
                    return "Invalid variable name '" + fullName + "' on line " + line
                            + ". Variable names can only contain letters (A-Z, a-z), digits (0-9), "
                            + "and underscores, and must not start with an underscore";
                }

                return "Malformed variable declaration on line " + line
                        + ". Expected '=' after variable name '" + attemptedName
                        + "', but found '" + t.get(2).text() + "'. "
                        + "Correct syntax: var " + attemptedName + " = <value>";
            }

            return "Invalid variable declaration on line " + line
                    + ". Expected: var <name> = <value>";
        }

        return "Unknown statement on line " + line
                + ". No matching statement or expression pattern was found";
    }

    /**
     * Classifies a {@code global [stored] var} statement into a {@link TypedStatement.GlobalVarStmt}.
     *
     * <p>Supports both {@code for <name>} (legacy) and {@code for ref type <name>} syntax.
     */
    private static @NotNull TypedStatement classifyNewGlobal(
            @NotNull StatementNode st,
            @NotNull List<Token> t,
            @NotNull PatternRegistry reg,
            @NotNull TypeEnv env) {
        int idx = 1;
        boolean stored = false;
        if (t.get(idx).text().equalsIgnoreCase("stored")) {
            stored = true;
            idx++;
        }
        idx++;

        String name = t.get(idx).text();
        idx++;

        String refTypeName = null;
        if (idx < t.size() && t.get(idx).text().equalsIgnoreCase("for")) {
            idx++;
            if (idx + 1 < t.size()
                    && t.get(idx).text().equalsIgnoreCase("ref")
                    && t.get(idx + 1).text().equalsIgnoreCase("type")) {
                idx += 2;
            }
            refTypeName = t.get(idx).text();
            idx++;
        }

        idx++;

        List<Token> exprTokens = t.subList(idx, t.size());
        RegisteredExpressionMatch globalExprMatch = reg.matchExpressionSlow(exprTokens, env);
        if (globalExprMatch == null) {
            globalExprMatch = reg.matchExpression(exprTokens, env);
        }
        Expr expr = ExprParser.parse(exprTokens, env);
        return new TypedStatement.GlobalVarStmt(st, name, refTypeName, expr, globalExprMatch, stored);
    }

    /**
     * Classifies a {@code stored var} statement into a {@link TypedStatement.StoreVarStmt}.
     *
     * <p>Supports both {@code for <name>} (legacy) and {@code for ref type <name>} syntax.
     */
    private static @NotNull TypedStatement classifyStoredVar(
            @NotNull StatementNode st,
            @NotNull List<Token> t,
            @NotNull TypeEnv env) {
        String name = t.get(2).text();
        String scopeVar = null;
        int defaultIdx;

        if (t.get(3).text().equalsIgnoreCase("for")) {
            int forIdx = 4;
            if (forIdx + 1 < t.size()
                    && t.get(forIdx).text().equalsIgnoreCase("ref")
                    && t.get(forIdx + 1).text().equalsIgnoreCase("type")) {
                forIdx += 2;
            }
            scopeVar = t.get(forIdx).text();
            defaultIdx = forIdx + 1;
        } else {
            defaultIdx = 3;
        }

        List<Token> exprTokens = t.subList(defaultIdx + 1, t.size());
        Expr expr = ExprParser.parse(exprTokens, env);
        return new TypedStatement.StoreVarStmt(st, name, scopeVar, expr);
    }

    /**
     * Checks whether the token list represents a {@code var x = <expr>} assignment.
     */
    private static boolean isVarStatement(@NotNull List<Token> t) {
        return t.size() >= 4
                && t.get(0).text().equalsIgnoreCase("var")
                && t.get(2).text().equals("=");
    }

    /**
     * Checks whether the token list represents a legacy {@code store} statement.
     *
     * <p>
     * Supported forms:
     * <ul>
     * <li>{@code store x default <expr>}</li>
     * <li>{@code store x for <scope> default <expr>}</li>
     * </ul>
     */
    private static boolean isStoreStatement(@NotNull List<Token> t) {
        if (t.size() < 4 || !t.get(0).text().equalsIgnoreCase("store"))
            return false;
        for (int i = 2; i < t.size(); i++) {
            if (t.get(i).text().equalsIgnoreCase("default"))
                return true;
        }
        return false;
    }

    /**
     * Checks whether the token list represents a {@code stored var} statement.
     *
     * <p>
     * Supported forms:
     * <ul>
     * <li>{@code stored var x default <expr>}</li>
     * <li>{@code stored var x for <scope> default <expr>}</li>
     * <li>{@code stored var x for ref type <scope> default <expr>}</li>
     * </ul>
     */
    private static boolean isStoredVarStatement(@NotNull List<Token> t) {
        if (t.size() < 5
                || !t.get(0).text().equalsIgnoreCase("stored")
                || !t.get(1).text().equalsIgnoreCase("var"))
            return false;
        for (int i = 3; i < t.size(); i++) {
            if (t.get(i).text().equalsIgnoreCase("default"))
                return true;
        }
        return false;
    }

    /**
     * Checks whether the token list represents a new {@code global [stored] var} declaration.
     *
     * <p>
     * Supported forms:
     * <ul>
     * <li>{@code global var x default <expr>}</li>
     * <li>{@code global var x for <refType> default <expr>}</li>
     * <li>{@code global var x for ref type <refType> default <expr>}</li>
     * <li>{@code global stored var x default <expr>}</li>
     * <li>{@code global stored var x for <refType> default <expr>}</li>
     * <li>{@code global stored var x for ref type <refType> default <expr>}</li>
     * </ul>
     */
    private static boolean isNewGlobalStatement(@NotNull List<Token> t) {
        if (t.size() < 5 || !t.get(0).text().equalsIgnoreCase("global"))
            return false;

        int idx = 1;
        if (t.get(idx).text().equalsIgnoreCase("stored")) {
            idx++;
        }
        if (!t.get(idx).text().equalsIgnoreCase("var"))
            return false;
        String nameCandidate = t.get(idx + 1).text();
        if (nameCandidate.equalsIgnoreCase("for") || nameCandidate.equalsIgnoreCase("default")) {
            return false;
        }
        idx += 2;

        for (int i = idx; i < t.size(); i++) {
            if (t.get(i).text().equalsIgnoreCase("default"))
                return true;
        }
        return false;
    }

    /**
     * Checks whether the token list represents a legacy {@code global} declaration.
     *
     * <p>
     * Supported forms:
     * <ul>
     * <li>{@code global x default <expr>}</li>
     * <li>{@code global x for <refType> default <expr>}</li>
     * </ul>
     */
    private static boolean isGlobalStatement(@NotNull List<Token> t) {
        if (t.size() < 4 || !t.get(0).text().equalsIgnoreCase("global"))
            return false;
        if (t.get(2).text().equalsIgnoreCase("default"))
            return true;
        return t.size() >= 6
                && t.get(2).text().equalsIgnoreCase("for")
                && t.get(4).text().equalsIgnoreCase("default");
    }
}
