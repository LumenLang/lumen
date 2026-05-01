package dev.lumenlang.console.element;

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
     */
    public static @NotNull String render(@NotNull Element root, int defaultWidth, int defaultHeight) {
        int w = root.width() < 0 ? defaultWidth : root.width();
        int h = root.height() < 0 ? defaultHeight : root.height();
        String[] lines = root.render(w, h);
        return String.join("\n", lines);
    }

    /**
     * Renders {@code root} into a newline-joined string using its preferred size. The element must
     * not have flexible dimensions.
     */
    public static @NotNull String render(@NotNull Element root) {
        return String.join("\n", root.render(root.width(), root.height()));
    }

    /**
     * Renders {@code root} using the current terminal columns as the flex-width default and a
     * height of 1 as the flex-height default.
     */
    public static @NotNull String renderToTerminal(@NotNull Element root) {
        return render(root, TerminalCapabilities.columns(), 1);
    }
}
