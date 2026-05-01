package dev.lumenlang.console.element.impl.widget;

import dev.lumenlang.console.border.BorderStyle;
import dev.lumenlang.console.element.Element;
import dev.lumenlang.console.element.impl.basic.Bordered;
import dev.lumenlang.console.element.impl.basic.Padded;
import dev.lumenlang.console.element.layout.Column;
import dev.lumenlang.console.element.layout.Row;
import dev.lumenlang.console.style.Color;
import dev.lumenlang.console.style.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Bordered container with an optional header and a body. Default styling is a light border with
 * one cell of padding inside.
 */
public final class Card implements Element {

    private final @Nullable Element header;
    private final @NotNull Element body;
    private final @NotNull BorderStyle border;
    private final @NotNull Style borderStyle;
    private final int padV;
    private final int padH;

    private Card(@Nullable Element header, @NotNull Element body, @NotNull BorderStyle border, @NotNull Style borderStyle, int padV, int padH) {
        this.header = header;
        this.body = body;
        this.border = border;
        this.borderStyle = borderStyle;
        this.padV = padV;
        this.padH = padH;
    }

    /**
     * Builds a card with the given body and no header.
     */
    public static @NotNull Card of(@NotNull Element body) {
        return new Card(null, body, BorderStyle.LIGHT, Style.fg(Color.SLATE), 1, 1);
    }

    /**
     * Returns a copy with the given header element above the body.
     */
    public @NotNull Card header(@NotNull Element header) {
        return new Card(header, body, border, borderStyle, padV, padH);
    }

    /**
     * Returns a copy with a different border glyph set.
     */
    public @NotNull Card border(@NotNull BorderStyle border) {
        return new Card(header, body, border, borderStyle, padV, padH);
    }

    /**
     * Returns a copy with the border colored differently.
     */
    public @NotNull Card borderColor(@NotNull Color color) {
        return new Card(header, body, border, borderStyle.withFg(color), padV, padH);
    }

    /**
     * Returns a copy with the border style replaced entirely.
     */
    public @NotNull Card borderStyle(@NotNull Style style) {
        return new Card(header, body, border, style, padV, padH);
    }

    /**
     * Returns a copy with vertical and horizontal interior padding adjusted.
     */
    public @NotNull Card padding(int vertical, int horizontal) {
        return new Card(header, body, border, borderStyle, vertical, horizontal);
    }

    private @NotNull Element compose() {
        Element inner = header == null ? body : Column.of(header, Row.of(), body);
        Element padded = Padded.symmetric(inner, padV, padH);
        return Bordered.of(padded).border(border).style(borderStyle);
    }

    @Override
    public int width() {
        return compose().width();
    }

    @Override
    public int height() {
        return compose().height();
    }

    @Override
    public @NotNull String @NotNull [] render(int width, int height) {
        return compose().render(width, height);
    }
}
