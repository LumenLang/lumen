package dev.lumenlang.console.element.impl.basic;

import dev.lumenlang.console.element.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Empty filler with explicit dimensions. Use {@code -1} for either dimension to flex.
 */
public final class Spacer implements Element {

    private final int w;
    private final int h;

    private Spacer(int w, int h) {
        this.w = w;
        this.h = h;
    }

    /**
     * Builds a spacer with the given width and height. Either may be {@code -1} to flex.
     */
    public static @NotNull Spacer of(int width, int height) {
        return new Spacer(width, height);
    }

    /**
     * Builds a horizontal spacer of the given width and height 1.
     */
    public static @NotNull Spacer horizontal(int width) {
        return new Spacer(width, 1);
    }

    /**
     * Builds a vertical spacer of the given height and width 1.
     */
    public static @NotNull Spacer vertical(int height) {
        return new Spacer(1, height);
    }

    /**
     * Builds a spacer that flexes on both axes.
     */
    public static @NotNull Spacer flex() {
        return new Spacer(-1, -1);
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
        String blank = " ".repeat(width);
        String[] out = new String[height];
        Arrays.fill(out, blank);
        return out;
    }
}
