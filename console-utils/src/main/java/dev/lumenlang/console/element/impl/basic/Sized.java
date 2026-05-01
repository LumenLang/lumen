package dev.lumenlang.console.element.impl.basic;

import dev.lumenlang.console.element.Element;
import org.jetbrains.annotations.NotNull;

/**
 * Forces a child to a fixed size, overriding its preferred dimensions. The child still controls
 * what fills that area; this element only clamps the layout.
 */
public final class Sized implements Element {

    private final @NotNull Element child;
    private final int w;
    private final int h;

    private Sized(@NotNull Element child, int w, int h) {
        this.child = child;
        this.w = w;
        this.h = h;
    }

    /**
     * Pins the child to the given width and height.
     */
    public static @NotNull Sized of(@NotNull Element child, int width, int height) {
        return new Sized(child, width, height);
    }

    /**
     * Pins the child to the given width, leaving height to its preference.
     */
    public static @NotNull Sized width(@NotNull Element child, int width) {
        return new Sized(child, width, child.height());
    }

    /**
     * Pins the child to the given height, leaving width to its preference.
     */
    public static @NotNull Sized height(@NotNull Element child, int height) {
        return new Sized(child, child.width(), height);
    }

    /**
     * Pins width to 8 columns. For inline glyphs, single-word labels.
     */
    public static @NotNull Sized tiny(@NotNull Element child) {
        return width(child, 8);
    }

    /**
     * Pins width to 16 columns. For short labels and compact widgets.
     */
    public static @NotNull Sized small(@NotNull Element child) {
        return width(child, 16);
    }

    /**
     * Pins width to 32 columns. For default-sized progress bars and value rows.
     */
    public static @NotNull Sized medium(@NotNull Element child) {
        return width(child, 32);
    }

    /**
     * Pins width to 48 columns. For wide bars, headers, paragraph blocks.
     */
    public static @NotNull Sized large(@NotNull Element child) {
        return width(child, 48);
    }

    /**
     * Pins width to 64 columns. For full-width banners and dashboards.
     */
    public static @NotNull Sized huge(@NotNull Element child) {
        return width(child, 64);
    }

    /**
     * Marks the child as flex-width so its parent stretches it to fill available space.
     */
    public static @NotNull Sized fill(@NotNull Element child) {
        return new Sized(child, -1, child.height());
    }

    @Override
    public int width() {
        return w;
    }

    @Override
    public int height() {
        return h;
    }

    @Override
    public @NotNull String @NotNull [] render(int width, int height) {
        return child.render(width, height);
    }
}
