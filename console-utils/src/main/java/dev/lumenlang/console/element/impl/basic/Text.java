package dev.lumenlang.console.element.impl.basic;

import dev.lumenlang.console.element.Align;
import dev.lumenlang.console.element.Element;
import dev.lumenlang.console.element.util.VisibleLength;
import dev.lumenlang.console.style.Color;
import dev.lumenlang.console.style.Style;
import org.jetbrains.annotations.NotNull;

/**
 * A single line of styled text. Wider boxes pad with spaces; narrower boxes clip.
 */
public final class Text implements Element {

    private final @NotNull String content;
    private final @NotNull Style style;
    private final @NotNull Align align;

    private Text(@NotNull String content, @NotNull Style style, @NotNull Align align) {
        this.content = content;
        this.style = style;
        this.align = align;
    }

    /**
     * Builds a left-aligned, unstyled text element.
     */
    public static @NotNull Text of(@NotNull String content) {
        return new Text(content, Style.NONE, Align.START);
    }

    /**
     * Returns a copy with the given style.
     */
    public @NotNull Text style(@NotNull Style style) {
        return new Text(content, style, align);
    }

    /**
     * Returns a copy with the given alignment.
     */
    public @NotNull Text align(@NotNull Align align) {
        return new Text(content, style, align);
    }

    /**
     * Returns a copy with the given foreground color, preserving other style flags.
     */
    public @NotNull Text fg(@NotNull Color color) {
        return new Text(content, style.withFg(color), align);
    }

    /**
     * Returns a copy with the given background color, preserving other style flags.
     */
    public @NotNull Text bg(@NotNull Color color) {
        return new Text(content, style.withBg(color), align);
    }

    /**
     * Returns a copy with bold added to the existing style.
     */
    public @NotNull Text bold() {
        return new Text(content, style.bold(), align);
    }

    /**
     * Returns a copy with dim added to the existing style.
     */
    public @NotNull Text dim() {
        return new Text(content, style.dim(), align);
    }

    /**
     * Returns a copy with italic added to the existing style.
     */
    public @NotNull Text italic() {
        return new Text(content, style.italic(), align);
    }

    /**
     * Returns a copy with underline added to the existing style.
     */
    public @NotNull Text underline() {
        return new Text(content, style.underline(), align);
    }

    /**
     * Returns a copy aligned to the start of its container.
     */
    public @NotNull Text start() {
        return align(Align.START);
    }

    /**
     * Returns a copy centered within its container.
     */
    public @NotNull Text center() {
        return align(Align.CENTER);
    }

    /**
     * Returns a copy aligned to the end of its container.
     */
    public @NotNull Text end() {
        return align(Align.END);
    }

    @Override
    public int width() {
        return VisibleLength.of(content);
    }

    @Override
    public int height() {
        return 1;
    }

    @Override
    public @NotNull String @NotNull [] render(int width, int height) {
        String styled = style.apply(content);
        int visible = VisibleLength.of(content);
        String line;
        if (visible >= width) {
            line = VisibleLength.clip(styled, width);
        } else {
            int pad = width - visible;
            line = switch (align) {
                case START -> styled + " ".repeat(pad);
                case END -> " ".repeat(pad) + styled;
                case CENTER -> " ".repeat(pad / 2) + styled + " ".repeat(pad - pad / 2);
            };
        }
        String[] out = new String[Math.max(1, height)];
        out[0] = line;
        for (int i = 1; i < out.length; i++) out[i] = " ".repeat(width);
        return out;
    }
}
