package dev.lumenlang.console.element;

import org.jetbrains.annotations.NotNull;

/**
 * Composable terminal renderable. Every visible piece of console output is an {@code Element}.
 *
 * <p>An element knows its preferred width and height but renders into an arbitrary box given by its
 * parent. Children clip or pad as needed. Implementations are expected to be immutable; styling and
 * layout produce new instances rather than mutating.
 */
public interface Element {

    /**
     * Returns the preferred width in visible columns, ignoring ANSI escapes. {@code -1} means
     * flexible (consume what the parent gives).
     */
    int width();

    /**
     * Returns the preferred height in lines. {@code -1} means flexible.
     */
    int height();

    /**
     * Renders this element into a fixed-size box. The returned array must contain exactly
     * {@code height} entries, each of which has visible length exactly {@code width}.
     *
     * @param width  the assigned width in visible columns
     * @param height the assigned height in lines
     */
    @NotNull String @NotNull [] render(int width, int height);
}
