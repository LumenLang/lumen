package dev.lumenlang.lumen.api.emit;

import dev.lumenlang.lumen.api.codegen.HandlerContext;
import org.jetbrains.annotations.NotNull;

/**
 * A hook that runs every time a pattern-matched block exits, after the block's children
 * have been emitted but before the block handler's {@code end} method.
 */
@FunctionalInterface
public interface BlockExitHook {

    /**
     * Called after a block's children are emitted and before its {@code end} handler.
     *
     * @param ctx the handler context providing environment, codegen, and output access
     */
    void onBlockExit(@NotNull HandlerContext ctx);
}
