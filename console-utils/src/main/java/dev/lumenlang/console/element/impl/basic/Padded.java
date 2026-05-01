package dev.lumenlang.console.element.impl.basic;

import dev.lumenlang.console.element.Element;
import org.jetbrains.annotations.NotNull;

/**
 * Wraps a child with blank space on each side. Pads the child's render output without altering its
 * style.
 */
public final class Padded implements Element {

    private final @NotNull Element child;
    private final int top;
    private final int right;
    private final int bottom;
    private final int left;

    private Padded(@NotNull Element child, int top, int right, int bottom, int left) {
        this.child = child;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
    }

    /**
     * Pads all four sides equally.
     */
    public static @NotNull Padded all(@NotNull Element child, int padding) {
        return new Padded(child, padding, padding, padding, padding);
    }

    /**
     * Pads symmetrically: {@code vertical} on top and bottom, {@code horizontal} on left and right.
     */
    public static @NotNull Padded symmetric(@NotNull Element child, int vertical, int horizontal) {
        return new Padded(child, vertical, horizontal, vertical, horizontal);
    }

    /**
     * Pads with explicit values for each side.
     */
    public static @NotNull Padded of(@NotNull Element child, int top, int right, int bottom, int left) {
        return new Padded(child, top, right, bottom, left);
    }

    @Override
    public int width() {
        int cw = child.width();
        return cw < 0 ? -1 : cw + left + right;
    }

    @Override
    public int height() {
        int ch = child.height();
        return ch < 0 ? -1 : ch + top + bottom;
    }

    @Override
    public @NotNull String @NotNull [] render(int width, int height) {
        int innerWidth = Math.max(0, width - left - right);
        int innerHeight = Math.max(0, height - top - bottom);
        String[] inner = child.render(innerWidth, innerHeight);
        String[] out = new String[height];
        for (int i = 0; i < top; i++) out[i] = " ".repeat(width);
        for (int i = 0; i < innerHeight; i++) {
            String row = i < inner.length ? inner[i] : " ".repeat(innerWidth);
            out[top + i] = " ".repeat(left) + row + " ".repeat(right);
        }
        for (int i = 0; i < bottom; i++) out[top + innerHeight + i] = " ".repeat(width);
        return out;
    }
}
