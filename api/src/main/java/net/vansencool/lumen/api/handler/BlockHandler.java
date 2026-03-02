package net.vansencool.lumen.api.handler;

import net.vansencool.lumen.api.codegen.BindingAccess;
import net.vansencool.lumen.api.codegen.JavaOutput;
import net.vansencool.lumen.api.pattern.PatternRegistrar;
import org.jetbrains.annotations.NotNull;

/**
 * Handles a matched block pattern by emitting Java code around the block's children.
 *
 * <p>{@link #begin} is called before processing children; {@link #end} is called after.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * api.patterns().block("repeat %n:INT% times", new BlockHandler() {
 *     public void begin(BindingAccess ctx, JavaOutput out) {
 *         out.line("for (int i = 0; i < " + ctx.java("n") + "; i++) {");
 *     }
 *     public void end(BindingAccess ctx, JavaOutput out) {
 *         out.line("}");
 *     }
 * });
 * }</pre>
 *
 * @see PatternRegistrar#block(String, BlockHandler)
 */
public interface BlockHandler {

    /**
     * Called before any children are processed. Emit opening Java code here.
     *
     * @param ctx the bound parameters from the matched block header
     * @param out the builder to append Java source lines to
     */
    void begin(@NotNull BindingAccess ctx, @NotNull JavaOutput out);

    /**
     * Called after all children have been processed. Emit closing Java code here.
     *
     * @param ctx the same bound parameters as in {@link #begin}
     * @param out the builder to append Java source lines to
     */
    void end(@NotNull BindingAccess ctx, @NotNull JavaOutput out);
}
