package dev.lumenlang.lumen.api.handler;

import dev.lumenlang.lumen.api.codegen.HandlerContext;
import org.jetbrains.annotations.NotNull;

/**
 * Handles a matched block pattern by emitting Java code around the block's children.
 *
 * <p>{@link #begin} is called before processing children; {@link #end} is called after.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * api.patterns().block("repeat %n:INT% times", new BlockHandler() {
 *     public void begin(HandlerContext ctx) {
 *         ctx.out().line("for (int i = 0; i < " + ctx.java("n") + "; i++) {");
 *     }
 *     public void end(HandlerContext ctx) {
 *         ctx.out().line("}");
 *     }
 * });
 * }</pre>
 */
public interface BlockHandler {

    /**
     * Called before any children are processed. Emit opening Java code here.
     *
     * @param ctx the handler context providing bound parameters, output, and environment
     */
    void begin(@NotNull HandlerContext ctx);

    /**
     * Called after all children have been processed. Emit closing Java code here.
     *
     * @param ctx the handler context providing bound parameters, output, and environment
     */
    void end(@NotNull HandlerContext ctx);
}
