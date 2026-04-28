package dev.lumenlang.lumen.api.emit;

import dev.lumenlang.lumen.api.codegen.HandlerContext;
import org.jetbrains.annotations.NotNull;

/**
 * A hook that runs every time a pattern-matched block is entered, after the block handler's
 * {@code begin} method but before the block's children are emitted.
 *
 * <p>This is useful for injecting code that must appear at the top of every block body,
 * such as loading global variables into local scope.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * api.emitters().blockEnterHook(ctx -> {
 *     ctx.out().line("// block entered at line " + ctx.line());
 * });
 * }</pre>
 *
 * @see EmitRegistrar#blockEnterHook(BlockEnterHook)
 */
@FunctionalInterface
public interface BlockEnterHook {

    /**
     * Called after a block's {@code begin} handler and before its children are emitted.
     *
     * @param ctx the handler context providing environment, codegen, and output access
     */
    void onBlockEnter(@NotNull HandlerContext ctx);
}
