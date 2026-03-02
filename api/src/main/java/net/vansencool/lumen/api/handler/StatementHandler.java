package net.vansencool.lumen.api.handler;

import net.vansencool.lumen.api.codegen.BindingAccess;
import net.vansencool.lumen.api.codegen.JavaOutput;
import net.vansencool.lumen.api.pattern.PatternRegistrar;
import org.jetbrains.annotations.NotNull;

/**
 * Handles a matched statement pattern by emitting the corresponding Java code.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * api.patterns().statement("explode %who:PLAYER%", (line, ctx, out) ->
 *     out.line(ctx.java("who") + ".getWorld().createExplosion(" +
 *              ctx.java("who") + ".getLocation(), 4F);")
 * );
 * }</pre>
 *
 * @see PatternRegistrar#statement(String, StatementHandler)
 */
@FunctionalInterface
public interface StatementHandler {

    /**
     * Generates Java code for the matched statement.
     *
     * @param line the source line number of the statement
     * @param ctx  the bound parameters from the pattern match
     * @param out  the builder to append Java source lines to
     */
    void handle(int line, @NotNull BindingAccess ctx, @NotNull JavaOutput out);
}
