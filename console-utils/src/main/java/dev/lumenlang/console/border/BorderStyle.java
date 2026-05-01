package dev.lumenlang.console.border;

import org.jetbrains.annotations.NotNull;

/**
 * The nine glyphs that make up a rectangular border. Custom styles are constructed directly via the
 * record constructor; common styles are exposed as static instances.
 */
public record BorderStyle(@NotNull String topLeft, @NotNull String top, @NotNull String topRight, @NotNull String left, @NotNull String right, @NotNull String bottomLeft, @NotNull String bottom, @NotNull String bottomRight, @NotNull String horizontalDivider, @NotNull String verticalDivider) {

    /**
     * Heavy box drawing characters. The default banner look.
     */
    public static final BorderStyle HEAVY = new BorderStyle("╔", "═", "╗", "║", "║", "╚", "═", "╝", "═", "║");

    /**
     * Light box drawing characters. Subtler than heavy, good for nested boxes.
     */
    public static final BorderStyle LIGHT = new BorderStyle("┌", "─", "┐", "│", "│", "└", "─", "┘", "─", "│");

    /**
     * Rounded corners with light sides. Modern look in rich-supporting terminals.
     */
    public static final BorderStyle ROUNDED = new BorderStyle("╭", "─", "╮", "│", "│", "╰", "─", "╯", "─", "│");

    /**
     * Double-ruled box drawing.
     */
    public static final BorderStyle DOUBLE = new BorderStyle("╔", "═", "╗", "║", "║", "╚", "═", "╝", "═", "║");

    /**
     * Plain ASCII fallback for terminals without box drawing support.
     */
    public static final BorderStyle ASCII = new BorderStyle("+", "-", "+", "|", "|", "+", "-", "+", "-", "|");
}
