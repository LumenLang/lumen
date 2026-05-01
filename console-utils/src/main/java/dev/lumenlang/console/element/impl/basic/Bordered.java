package dev.lumenlang.console.element.impl.basic;

import dev.lumenlang.console.border.BorderStyle;
import dev.lumenlang.console.element.Element;
import dev.lumenlang.console.style.Style;
import org.jetbrains.annotations.NotNull;

/**
 * Wraps a child element with a border on all four sides. The border consumes one column on the
 * left and right, and one line on top and bottom.
 */
public final class Bordered implements Element {

    private final @NotNull Element child;
    private final @NotNull BorderStyle border;
    private final @NotNull Style style;

    private Bordered(@NotNull Element child, @NotNull BorderStyle border, @NotNull Style style) {
        this.child = child;
        this.border = border;
        this.style = style;
    }

    /**
     * Wraps the given child with the heavy border style and no color.
     */
    public static @NotNull Bordered of(@NotNull Element child) {
        return new Bordered(child, BorderStyle.HEAVY, Style.NONE);
    }

    /**
     * Returns a copy with a different border style.
     */
    public @NotNull Bordered border(@NotNull BorderStyle border) {
        return new Bordered(child, border, style);
    }

    /**
     * Returns a copy with the border glyphs styled (e.g. colored).
     */
    public @NotNull Bordered style(@NotNull Style style) {
        return new Bordered(child, border, style);
    }

    @Override
    public int width() {
        int cw = child.width();
        return cw < 0 ? -1 : cw + 2;
    }

    @Override
    public int height() {
        int ch = child.height();
        return ch < 0 ? -1 : ch + 2;
    }

    @Override
    public @NotNull String @NotNull [] render(int width, int height) {
        int innerWidth = Math.max(0, width - 2);
        int innerHeight = Math.max(0, height - 2);
        String[] inner = child.render(innerWidth, innerHeight);
        String[] out = new String[height];
        out[0] = style.apply(border.topLeft() + border.top().repeat(innerWidth) + border.topRight());
        for (int i = 0; i < innerHeight; i++) {
            String row = i < inner.length ? inner[i] : " ".repeat(innerWidth);
            out[i + 1] = style.apply(border.left()) + row + style.apply(border.right());
        }
        out[height - 1] = style.apply(border.bottomLeft() + border.bottom().repeat(innerWidth) + border.bottomRight());
        return out;
    }
}
