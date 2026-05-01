package dev.lumenlang.console.element.impl.widget;

import dev.lumenlang.console.element.Element;
import dev.lumenlang.console.element.impl.basic.Text;
import dev.lumenlang.console.element.layout.Row;
import dev.lumenlang.console.style.Color;
import dev.lumenlang.console.style.Style;
import org.jetbrains.annotations.NotNull;

/**
 * Compact label-value badge: dim label, bold colored value. Useful for status counters and
 * inline metrics.
 */
public final class Pill implements Element {

    private final @NotNull String label;
    private final @NotNull String value;
    private final @NotNull Style labelStyle;
    private final @NotNull Style valueStyle;

    private Pill(@NotNull String label, @NotNull String value, @NotNull Style labelStyle, @NotNull Style valueStyle) {
        this.label = label;
        this.value = value;
        this.labelStyle = labelStyle;
        this.valueStyle = valueStyle;
    }

    /**
     * Builds a pill with the given label and value, using muted-grey label and bold colored value.
     */
    public static @NotNull Pill of(@NotNull String label, @NotNull String value, @NotNull Color valueColor) {
        return new Pill(label, value, Style.fg(Color.GHOST_GREY), Style.fg(valueColor).bold());
    }

    /**
     * Returns a copy with the label style changed.
     */
    public @NotNull Pill labelStyle(@NotNull Style style) {
        return new Pill(label, value, style, valueStyle);
    }

    /**
     * Returns a copy with the value style changed.
     */
    public @NotNull Pill valueStyle(@NotNull Style style) {
        return new Pill(label, value, labelStyle, style);
    }

    private @NotNull Row asRow() {
        return Row.of(
                Text.of(label + " ").style(labelStyle),
                Text.of(value).style(valueStyle)
        );
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
