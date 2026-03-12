package net.vansencool.lumen.pipeline.language.typed;

import net.vansencool.lumen.pipeline.language.nodes.StatementNode;
import net.vansencool.lumen.pipeline.language.pattern.RegisteredExpressionMatch;
import net.vansencool.lumen.pipeline.language.pattern.RegisteredPatternMatch;
import net.vansencool.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A typed statement that has been classified into a specific category (e.g. pattern match, variable declaration, etc.)
 * and carries the relevant parsed information for code generation.
 */
public sealed interface TypedStatement permits
        TypedStatement.PatternStmt,
        TypedStatement.VarStmt,
        TypedStatement.ExprVarStmt,
        TypedStatement.ExprStmt,
        TypedStatement.StoreVarStmt,
        TypedStatement.GlobalVarStmt,
        TypedStatement.ErrorStmt {

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
     * A simple {@code var x = <expr>} statement.
     *
     * @param raw  the original statement node
     * @param name the variable name
     * @param expr the parsed expression
     */
    record VarStmt(StatementNode raw, String name, Expr expr) implements TypedStatement {
    }

    /**
     * A {@code var x = <expression pattern>} statement where the RHS matched a
     * registered
     * expression pattern.
     *
     * @param raw   the original statement node
     * @param name  the variable name
     * @param match the expression pattern match
     */
    record ExprVarStmt(StatementNode raw, String name, RegisteredExpressionMatch match) implements TypedStatement {
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
     * A {@code store x default <expr>} or
     * {@code store x for <scope> default <expr>} statement
     * for persistent variables that survive restarts.
     *
     * <p>
     * When {@code scopeVar} is non-null the storage key is scoped to that
     * variable's
     * runtime identity (e.g. a player UUID), enabling per-entity persistent
     * storage.
     *
     * @param raw      the original statement node
     * @param name     the variable name
     * @param scopeVar the optional scope variable name (e.g. {@code "player"}), or {@code null}
     * @param expr     the parsed default value expression
     */
    record StoreVarStmt(StatementNode raw, String name, @Nullable String scopeVar,
                        Expr expr) implements TypedStatement {
    }

    /**
     * A {@code global [stored] var x default <expr>} or
     * {@code global [stored] var x for <refType> default <expr>}
     * declaration for script-wide variables.
     *
     * <p>
     * Global declarations are only valid at the script's top level. They register a
     * variable that is automatically loaded at the start of every method body.
     * When {@code stored} is {@code true}, the variable is persisted to disk and
     * auto-saved on modification. When {@code false}, the variable lives only in
     * memory for the duration of the server session.
     *
     * <p>
     * When {@code refTypeName} is non-null, the global is scoped to that
     * {@code RefType}.
     * Blocks that have a variable matching the given type will load the
     * global with
     * a scoped key; blocks that do not have that type simply skip loading this
     * variable.
     *
     * @param raw         the original statement node
     * @param name        the variable name
     * @param refTypeName the optional ref type name for per-entity scoping (e.g. {@code "player"}), or {@code null} for server-wide globals
     * @param expr        the parsed default value expression
     * @param exprMatch   the optional expression pattern match
     * @param stored      whether the variable is persisted to disk
     */
    record GlobalVarStmt(StatementNode raw, String name, @Nullable String refTypeName,
                         Expr expr, @Nullable RegisteredExpressionMatch exprMatch,
                         boolean stored) implements TypedStatement {
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
     */
    record ErrorStmt(@NotNull StatementNode raw, @NotNull String message,
                     @Nullable List<Token> errorTokens) implements TypedStatement {
    }
}
