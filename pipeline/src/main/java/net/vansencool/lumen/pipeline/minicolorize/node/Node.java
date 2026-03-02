package net.vansencool.lumen.pipeline.minicolorize.node;

import org.jetbrains.annotations.NotNull;

/**
 * A node in the parsed MiniColorize tree.
 */
public interface Node {

    /**
     * Returns a human-readable description of this node for debugging.
     *
     * @return a description string
     */
    @NotNull String describe();
}
