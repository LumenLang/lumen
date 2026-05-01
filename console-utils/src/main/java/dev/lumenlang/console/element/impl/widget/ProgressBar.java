package dev.lumenlang.console.element.impl.widget;

import dev.lumenlang.console.element.Element;
import dev.lumenlang.console.element.impl.basic.Sized;
import dev.lumenlang.console.style.Color;
import dev.lumenlang.console.style.Style;
import org.jetbrains.annotations.NotNull;

/**
 * Horizontal progress bar with a filled segment, a track segment, and configurable glyphs and
 * colors. Stateless: the {@code progress} value is supplied at construction. Pair with a live
 * renderer to drive updates over time.
 */
public final class ProgressBar implements Element {

    private final double progress;
    private final int barWidth;
    private final @NotNull String fillGlyph;
    private final @NotNull String trackGlyph;
    private final @NotNull Style fillStyle;
    private final @NotNull Style trackStyle;

    private ProgressBar(double progress, int barWidth, @NotNull String fillGlyph, @NotNull String trackGlyph, @NotNull Style fillStyle, @NotNull Style trackStyle) {
        this.progress = progress;
        this.barWidth = barWidth;
        this.fillGlyph = fillGlyph;
        this.trackGlyph = trackGlyph;
        this.fillStyle = fillStyle;
        this.trackStyle = trackStyle;
    }

    /**
     * Builds a default progress bar at the given progress (0.0 to 1.0) and bar width.
     */
    public static @NotNull ProgressBar of(double progress, int width) {
        return new ProgressBar(progress, width, "━", "─", Style.fg(Color.MINT), Style.fg(Color.SLATE).dim());
    }

    /**
     * Builds a flex-width progress bar at the given progress (0.0 to 1.0). The bar grows to fill
     * whatever width the parent assigns it. Wrap in {@link Sized}
     * to clamp to a specific size.
     */
    public static @NotNull ProgressBar of(double progress) {
        return new ProgressBar(progress, -1, "━", "─", Style.fg(Color.MINT), Style.fg(Color.SLATE).dim());
    }

    /**
     * Returns a copy with custom glyphs.
     */
    public @NotNull ProgressBar glyphs(@NotNull String fill, @NotNull String track) {
        return new ProgressBar(progress, barWidth, fill, track, fillStyle, trackStyle);
    }

    /**
     * Returns a copy with custom styles applied to the fill and track segments.
     */
    public @NotNull ProgressBar styles(@NotNull Style fillStyle, @NotNull Style trackStyle) {
        return new ProgressBar(progress, barWidth, fillGlyph, trackGlyph, fillStyle, trackStyle);
    }

    /**
     * Returns a copy with the bar width changed.
     */
    public @NotNull ProgressBar width(int width) {
        return new ProgressBar(progress, width, fillGlyph, trackGlyph, fillStyle, trackStyle);
    }

    @Override
    public int width() {
        return barWidth;
    }

    @Override
    public int height() {
        return 1;
    }

    @Override
    public @NotNull String @NotNull [] render(int width, int height) {
        double clamped = Math.max(0, Math.min(1, progress));
        int filled = (int) Math.round(clamped * width);
        int empty = width - filled;
        StringBuilder sb = new StringBuilder();
        if (filled > 0) sb.append(fillStyle.apply(fillGlyph.repeat(filled)));
        if (empty > 0) sb.append(trackStyle.apply(trackGlyph.repeat(empty)));
        String[] out = new String[Math.max(1, height)];
        out[0] = sb.toString();
        String blank = " ".repeat(width);
        for (int i = 1; i < out.length; i++) out[i] = blank;
        return out;
    }
}
