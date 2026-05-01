package dev.lumenlang.console;

import dev.lumenlang.console.border.BorderStyle;
import dev.lumenlang.console.element.Align;
import dev.lumenlang.console.element.Element;
import dev.lumenlang.console.element.impl.basic.Bordered;
import dev.lumenlang.console.element.impl.basic.Custom;
import dev.lumenlang.console.element.impl.basic.Padded;
import dev.lumenlang.console.element.impl.basic.Repeat;
import dev.lumenlang.console.element.impl.basic.Sized;
import dev.lumenlang.console.element.impl.basic.Spacer;
import dev.lumenlang.console.element.impl.basic.Text;
import dev.lumenlang.console.element.impl.widget.Card;
import dev.lumenlang.console.element.impl.widget.KeyValue;
import dev.lumenlang.console.element.impl.widget.Pill;
import dev.lumenlang.console.element.impl.widget.ProgressBar;
import dev.lumenlang.console.element.impl.widget.Spinner;
import dev.lumenlang.console.element.impl.widget.Tree;
import dev.lumenlang.console.element.layout.Column;
import dev.lumenlang.console.element.layout.Row;
import dev.lumenlang.console.style.Color;
import dev.lumenlang.console.style.Style;
import org.jetbrains.annotations.NotNull;

/**
 * Static facade with short factory methods for every element and common style operation. Designed
 * for {@code import static net.lumenlang.console.UIUtils.*;} so layout code reads close to a DSL.
 */
@SuppressWarnings("unused")
public final class UIUtils {

    /**
     * Shortcut for {@link Align#START}.
     */
    public static final Align START = Align.START;

    /**
     * Shortcut for {@link Align#CENTER}.
     */
    public static final Align CENTER = Align.CENTER;

    /**
     * Shortcut for {@link Align#END}.
     */
    public static final Align END = Align.END;

    private UIUtils() {
    }

    /**
     * Builds a styled text element.
     */
    public static @NotNull Text text(@NotNull String content) {
        return Text.of(content);
    }

    /**
     * Builds a row of children.
     */
    public static @NotNull Row row(@NotNull Element @NotNull ... children) {
        return Row.of(children);
    }

    /**
     * Builds a column of children.
     */
    public static @NotNull Column col(@NotNull Element @NotNull ... children) {
        return Column.of(children);
    }

    /**
     * Builds a fixed-size spacer with given width and height.
     */
    public static @NotNull Spacer spacer(int width, int height) {
        return Spacer.of(width, height);
    }

    /**
     * Builds a horizontal spacer of the given width.
     */
    public static @NotNull Spacer hspace(int width) {
        return Spacer.horizontal(width);
    }

    /**
     * Builds a vertical spacer of the given height.
     */
    public static @NotNull Spacer vspace(int height) {
        return Spacer.vertical(height);
    }

    /**
     * Builds a horizontal rule using the given glyph.
     */
    public static @NotNull Repeat rule(@NotNull String glyph) {
        return Repeat.of(glyph);
    }

    /**
     * Builds a progress bar at the given progress (0.0 to 1.0) and width.
     */
    public static @NotNull ProgressBar bar(double progress, int width) {
        return ProgressBar.of(progress, width);
    }

    /**
     * Builds a flex-width progress bar. Wrap in {@link #small}, {@link #medium}, {@link #large},
     * etc. to clamp to a named size.
     */
    public static @NotNull ProgressBar bar(double progress) {
        return ProgressBar.of(progress);
    }

    /**
     * Builds a braille spinner at the given frame index.
     */
    public static @NotNull Spinner spinner(int frame) {
        return Spinner.of(frame);
    }

    /**
     * Builds a key-value row.
     */
    public static @NotNull KeyValue kv(@NotNull String key, @NotNull String value) {
        return KeyValue.of(key, value);
    }

    /**
     * Builds a label-value pill with the given value color.
     */
    public static @NotNull Pill pill(@NotNull String label, @NotNull String value, @NotNull Color valueColor) {
        return Pill.of(label, value, valueColor);
    }

    /**
     * Builds a tree with the given top-level nodes.
     */
    public static @NotNull Tree tree(@NotNull Tree.Node @NotNull ... nodes) {
        return Tree.of(nodes);
    }

    /**
     * Builds a leaf tree node.
     */
    public static @NotNull Tree.Node leaf(@NotNull Element label) {
        return Tree.Node.leaf(label);
    }

    /**
     * Builds a branching tree node.
     */
    public static @NotNull Tree.Node branch(@NotNull Element label, @NotNull Tree.Node @NotNull ... children) {
        return Tree.Node.branch(label, children);
    }

    /**
     * Wraps a body in a default card (light border, slate, padded).
     */
    public static @NotNull Card card(@NotNull Element body) {
        return Card.of(body);
    }

    /**
     * Wraps a child in padding on all sides.
     */
    public static @NotNull Padded padded(@NotNull Element child, int padding) {
        return Padded.all(child, padding);
    }

    /**
     * Wraps a child with symmetric vertical and horizontal padding.
     */
    public static @NotNull Padded padded(@NotNull Element child, int vertical, int horizontal) {
        return Padded.symmetric(child, vertical, horizontal);
    }

    /**
     * Wraps a child with the given border style.
     */
    public static @NotNull Bordered bordered(@NotNull Element child, @NotNull BorderStyle border) {
        return Bordered.of(child).border(border);
    }

    /**
     * Wraps a child with a heavy border.
     */
    public static @NotNull Bordered bordered(@NotNull Element child) {
        return Bordered.of(child);
    }

    /**
     * Pins a child to a fixed size.
     */
    public static @NotNull Sized sized(@NotNull Element child, int width, int height) {
        return Sized.of(child, width, height);
    }

    /**
     * Pins width to 8 columns.
     */
    public static @NotNull Sized tiny(@NotNull Element child) {
        return Sized.tiny(child);
    }

    /**
     * Pins width to 16 columns.
     */
    public static @NotNull Sized small(@NotNull Element child) {
        return Sized.small(child);
    }

    /**
     * Pins width to 32 columns.
     */
    public static @NotNull Sized medium(@NotNull Element child) {
        return Sized.medium(child);
    }

    /**
     * Pins width to 48 columns.
     */
    public static @NotNull Sized large(@NotNull Element child) {
        return Sized.large(child);
    }

    /**
     * Pins width to 64 columns.
     */
    public static @NotNull Sized huge(@NotNull Element child) {
        return Sized.huge(child);
    }

    /**
     * Marks the child as flex-width so its parent stretches it to fill available space.
     */
    public static @NotNull Sized fill(@NotNull Element child) {
        return Sized.fill(child);
    }

    /**
     * Builds a custom-painted element.
     */
    public static @NotNull Custom custom(int width, int height, @NotNull Custom.Painter painter) {
        return Custom.of(width, height, painter);
    }


    /**
     * Builds a foreground style.
     */
    public static @NotNull Style fg(@NotNull Color color) {
        return Style.fg(color);
    }

    /**
     * Builds a background style.
     */
    public static @NotNull Style bg(@NotNull Color color) {
        return Style.bg(color);
    }
}
