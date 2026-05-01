package dev.lumenlang.console.element.impl.basic;

import dev.lumenlang.console.element.Element;
import dev.lumenlang.console.element.util.VisibleLength;
import org.jetbrains.annotations.NotNull;

/**
 * Escape hatch for arbitrary rendering. The supplied painter is called with the assigned size and
 * returns the rendered lines. Useful for ASCII art, custom widgets, or anything the layout system
 * doesn't model.
 */
public final class Custom implements Element {

    private final int w;
    private final int h;
    private final @NotNull Painter painter;

    private Custom(int w, int h, @NotNull Painter painter) {
        this.w = w;
        this.h = h;
        this.painter = painter;
    }

    /**
     * Builds a custom element with the given preferred dimensions and painter. Either dimension
     * may be {@code -1} to flex.
     */
    public static @NotNull Custom of(int width, int height, @NotNull Painter painter) {
        return new Custom(width, height, painter);
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
        String[] painted = painter.paint(width, height);
        String[] out = new String[height];
        for (int i = 0; i < height; i++) {
            String row = i < painted.length ? painted[i] : "";
            out[i] = VisibleLength.pad(row, width);
        }
        return out;
    }

    /**
     * Paints lines for a custom element. Implementations should return at most {@code height} rows
     * of at most {@code width} visible columns each; padding and clipping are handled afterward.
     */
    @FunctionalInterface
    public interface Painter {
        @NotNull String @NotNull [] paint(int width, int height);
    }
}
