package net.vansencool.lumen.pipeline.minicolorize.node;

import org.jetbrains.annotations.NotNull;

/**
 * A node containing plain text with no formatting tags.
 *
 * @param text the raw text content
 */
public record TextNode(@NotNull String text) implements Node {

    @Override
    public @NotNull String describe() {
        return "text(\"" + text + "\")";
    }
}
