package dev.lumenlang.console.element.impl.basic;

import dev.lumenlang.console.element.Element;
import dev.lumenlang.console.element.util.VisibleLength;
import dev.lumenlang.console.style.Style;
import org.jetbrains.annotations.NotNull;

/**
 * Repeats a glyph horizontally to fill its assigned width. Useful for rules, fills, and progress
 * bar tracks.
 */
public final class Repeat implements Element {

    private final @NotNull String glyph;
    private final @NotNull Style style;

    private Repeat(@NotNull String glyph, @NotNull Style style) {
        this.glyph = glyph;
        this.style = style;
    }

    /**
     * Builds an unstyled, flexible-width repeat of the given glyph.
     */
    public static @NotNull Repeat of(@NotNull String glyph) {
        return new Repeat(glyph, Style.NONE);
    }

    /**
     * Returns a copy with the given style applied to each repeated glyph.
     */
    public @NotNull Repeat style(@NotNull Style style) {
        return new Repeat(glyph, style);
    }

    @Override
    public int width() {
        return -1;
    }

    @Override
    public int height() {
        return 1;
    }

    @Override
    public @NotNull String @NotNull [] render(int width, int height) {
        int gw = Math.max(1, VisibleLength.of(glyph));
        int count = width / gw;
        String body = glyph.repeat(count);
        String styled = style.apply(VisibleLength.pad(body, width));
        String[] out = new String[Math.max(1, height)];
        out[0] = styled;
        String blank = " ".repeat(width);
        for (int i = 1; i < out.length; i++) out[i] = blank;
        return out;
    }
}
