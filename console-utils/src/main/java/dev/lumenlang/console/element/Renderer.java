package dev.lumenlang.console.element;

import dev.lumenlang.console.element.util.VisibleLength;
import dev.lumenlang.console.terminal.TerminalCapabilities;
import org.jetbrains.annotations.NotNull;

/**
 * Convenience helper that drives an {@link Element} to a final {@code String}. Handles the case
 * where the element prefers flexible dimensions by falling back to caller-supplied defaults.
 */
public final class Renderer {

    private Renderer() {
    }

    /**
     * Renders {@code root} into a newline-joined string. When the element's preferred width or
     * height is flexible ({@code -1}), {@code defaultWidth} or {@code defaultHeight} is used.
     * Trailing visible whitespace on each line is removed.
     */
    public static @NotNull String render(@NotNull Element root, int defaultWidth, int defaultHeight) {
        int w = root.width() < 0 ? defaultWidth : root.width();
        int h = root.height() < 0 ? defaultHeight : root.height();
        return joinStripped(root.render(w, h));
    }

    /**
     * Renders {@code root} into a newline-joined string using its preferred size. The element must
     * not have flexible dimensions. Trailing visible whitespace on each line is removed.
     */
    public static @NotNull String render(@NotNull Element root) {
        return joinStripped(root.render(root.width(), root.height()));
    }

    /**
     * Renders {@code root} using the current terminal columns as the flex-width default and a
     * height of 1 as the flex-height default.
     */
    public static @NotNull String renderToTerminal(@NotNull Element root) {
        return render(root, TerminalCapabilities.columns(), 1);
    }

    private static @NotNull String joinStripped(@NotNull String[] lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) sb.append('\n');
            sb.append(VisibleLength.stripTrailing(lines[i]));
        }
        return sb.toString();
    }
}
