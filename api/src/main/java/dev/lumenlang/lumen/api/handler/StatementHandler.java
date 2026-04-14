package dev.lumenlang.lumen.api.handler;

import dev.lumenlang.lumen.api.codegen.HandlerContext;
import org.jetbrains.annotations.NotNull;

/**
 * Handles a matched statement pattern by emitting the corresponding Java code.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * api.patterns().statement("explode %who:PLAYER%", ctx ->
 *     ctx.out().line(ctx.java("who") + ".getWorld().createExplosion(" +
 *              ctx.java("who") + ".getLocation(), 4F);")
 * );
 * }</pre>
 */
@FunctionalInterface
public interface StatementHandler {

    /**
     * Generates Java code for the matched statement.
     *
     * @param ctx the handler context providing bound parameters, output, and environment
     */
    void handle(@NotNull HandlerContext ctx);
}
