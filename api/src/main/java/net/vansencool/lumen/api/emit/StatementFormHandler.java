package net.vansencool.lumen.api.emit;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Handles a custom statement form by inspecting raw tokens and emitting Java code.
 *
 * <p>Statement form handlers are tried in registration order <em>before</em> pattern matching.
 * If a handler returns {@code true} from {@link #tryHandle}, the statement is considered
 * fully handled and no further matching is attempted.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * api.emitters().statementForm((tokens, ctx) -> {
 *     if (tokens.size() < 2 || !tokens.get(0).text().equalsIgnoreCase("debug")) {
 *         return false;
 *     }
 *     String message = tokens.get(1).text();
 *     ctx.out().line("System.out.println(\"DEBUG: " + message + "\");");
 *     return true;
 * });
 * }</pre>
 *
 * @see EmitRegistrar#statementForm(StatementFormHandler)
 */
@FunctionalInterface
public interface StatementFormHandler {

    /**
     * Attempts to handle the given statement tokens.
     *
     * <p>Implementations should inspect the tokens to decide whether they match the expected
     * form. If they do, the handler should emit the corresponding Java code via
     * {@link EmitContext#out()} and return {@code true}. If the tokens do not match,
     * return {@code false} without emitting anything.
     *
     * @param tokens the tokens of the statement line
     * @param ctx    the emit context providing environment, codegen, and output access
     * @return {@code true} if the statement was handled, {@code false} to try the next handler
     */
    boolean tryHandle(@NotNull List<? extends ScriptToken> tokens, @NotNull EmitContext ctx);
}
