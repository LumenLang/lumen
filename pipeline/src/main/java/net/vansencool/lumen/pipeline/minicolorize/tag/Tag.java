package net.vansencool.lumen.pipeline.minicolorize.tag;

import net.vansencool.lumen.pipeline.minicolorize.node.TagNode;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a parsed MiniColorize tag such as {@code <yellow>}, {@code <bold>}, or {@code <reset>}.
 *
 * @see TagResolver
 */
public interface Tag {

    /**
     * Returns a human readable representation of this tag for debugging.
     *
     * @return a debug string
     */
    @NotNull String describe();

    /**
     * Returns whether this tag is self-closing, meaning it does not wrap child nodes.
     *
     * <p>Self-closing tags like {@code <reset>} or {@code <key:key.jump>} produce a
     * {@link TagNode} with an empty child list.
     * Override this to return {@code true} for tags that should never consume children.
     *
     * @return true if this tag is self-closing
     */
    default boolean selfClosing() {
        return false;
    }
}
