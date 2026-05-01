package dev.lumenlang.console.element.impl.widget;

import dev.lumenlang.console.element.Element;
import dev.lumenlang.console.element.impl.basic.Sized;
import dev.lumenlang.console.element.impl.basic.Text;
import dev.lumenlang.console.element.layout.Row;
import dev.lumenlang.console.style.Color;
import dev.lumenlang.console.style.Style;
import org.jetbrains.annotations.NotNull;

/**
 * Two-column row of label and value, both styled. Useful for status displays, config dumps, and
 * key-value lists.
 */
public final class KeyValue implements Element {

    private final @NotNull String key;
    private final @NotNull String value;
    private final int keyWidth;
    private final @NotNull Style keyStyle;
    private final @NotNull Style valueStyle;

    private KeyValue(@NotNull String key, @NotNull String value, int keyWidth, @NotNull Style keyStyle, @NotNull Style valueStyle) {
        this.key = key;
        this.value = value;
        this.keyWidth = keyWidth;
        this.keyStyle = keyStyle;
        this.valueStyle = valueStyle;
    }

    /**
     * Builds a key-value pair with default key width (12) and muted key style.
     */
    public static @NotNull KeyValue of(@NotNull String key, @NotNull String value) {
        return new KeyValue(key, value, 12, Style.fg(Color.GHOST_GREY), Style.fg(Color.BONE).bold());
    }

    /**
     * Returns a copy with the key column width changed.
     */
    public @NotNull KeyValue keyWidth(int width) {
        return new KeyValue(key, value, width, keyStyle, valueStyle);
    }

    /**
     * Returns a copy with the value style changed.
     */
    public @NotNull KeyValue valueStyle(@NotNull Style style) {
        return new KeyValue(key, value, keyWidth, keyStyle, style);
    }

    /**
     * Returns a copy with the value foreground color changed, preserving other style flags.
     */
    public @NotNull KeyValue valueColor(@NotNull Color color) {
        return new KeyValue(key, value, keyWidth, keyStyle, valueStyle.withFg(color));
    }

    /**
     * Returns a copy with the key style changed.
     */
    public @NotNull KeyValue keyStyle(@NotNull Style style) {
        return new KeyValue(key, value, keyWidth, style, valueStyle);
    }

    private @NotNull Row asRow() {
        Text k = Text.of(key).style(keyStyle);
        Text v = Text.of(value).style(valueStyle);
        return Row.of(Sized.width(k, keyWidth), v);
    }

    @Override
    public int width() {
        return asRow().width();
    }

    @Override
    public int height() {
        return 1;
    }

    @Override
    public @NotNull String @NotNull [] render(int width, int height) {
        return asRow().render(width, height);
    }
}
