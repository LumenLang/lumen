package dev.lumenlang.console.element.layout;

import dev.lumenlang.console.element.Align;
import dev.lumenlang.console.element.Element;
import dev.lumenlang.console.element.util.VisibleLength;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Vertical stack of children. Each child gets the column's full width and its preferred height.
 * Flexible children (height {@code -1}) split the leftover height evenly.
 */
public final class Column implements Element {

    private final @NotNull List<Element> children;
    private final int gap;
    private final @NotNull Align align;

    private Column(@NotNull List<Element> children, int gap, @NotNull Align align) {
        this.children = List.copyOf(children);
        this.gap = gap;
        this.align = align;
    }

    /**
     * Builds a column with the given children, no gap, start aligned.
     */
    public static @NotNull Column of(@NotNull Element @NotNull ... children) {
        return new Column(List.of(children), 0, Align.START);
    }

    /**
     * Returns a copy with a vertical gap (blank lines) between children.
     */
    public @NotNull Column gap(int lines) {
        return new Column(children, lines, align);
    }

    /**
     * Returns a copy with horizontal alignment applied to each child within the column width.
     */
    public @NotNull Column align(@NotNull Align align) {
        return new Column(children, gap, align);
    }

    @Override
    public int width() {
        int max = 0;
        boolean anyFlex = false;
        for (Element c : children) {
            int w = c.width();
            if (w < 0) anyFlex = true;
            else if (w > max) max = w;
        }
        return anyFlex ? -1 : max;
    }

    @Override
    public int height() {
        int total = 0;
        boolean any = false;
        for (Element c : children) {
            int h = c.height();
            if (h < 0) return -1;
            total += h;
            any = true;
        }
        return total + (any ? gap * (children.size() - 1) : 0);
    }

    @Override
    public @NotNull String @NotNull [] render(int width, int height) {
        int fixedTotal = 0;
        int flexCount = 0;
        for (Element c : children) {
            int h = c.height();
            if (h < 0) flexCount++;
            else fixedTotal += h;
        }
        int gapTotal = gap * Math.max(0, children.size() - 1);
        int flexBudget = Math.max(0, height - fixedTotal - gapTotal);
        int flexEach = flexCount > 0 ? flexBudget / flexCount : 0;
        int flexExtra = flexCount > 0 ? flexBudget - flexEach * flexCount : 0;

        List<String> out = new ArrayList<>(height);
        for (int i = 0; i < children.size(); i++) {
            Element c = children.get(i);
            int h = c.height();
            int assigned;
            if (h < 0) {
                assigned = flexEach + (flexExtra-- > 0 ? 1 : 0);
            } else {
                assigned = h;
            }
            int childWidth = c.width() < 0 ? width : Math.min(c.width(), width);
            String[] rendered = c.render(childWidth, assigned);
            for (String row : rendered) out.add(alignRow(row, childWidth, width));
            if (i < children.size() - 1) {
                for (int g = 0; g < gap; g++) out.add(" ".repeat(width));
            }
        }
        while (out.size() < height) out.add(" ".repeat(width));
        if (out.size() > height) return out.subList(0, height).toArray(new String[0]);
        return out.toArray(new String[0]);
    }

    private @NotNull String alignRow(@NotNull String row, int childWidth, int totalWidth) {
        int pad = totalWidth - childWidth;
        if (pad <= 0) return VisibleLength.clip(row, totalWidth);
        return switch (align) {
            case START -> row + " ".repeat(pad);
            case END -> " ".repeat(pad) + row;
            case CENTER -> " ".repeat(pad / 2) + row + " ".repeat(pad - pad / 2);
        };
    }
}
