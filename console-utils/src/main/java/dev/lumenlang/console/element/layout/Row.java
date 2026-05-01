package dev.lumenlang.console.element.layout;

import dev.lumenlang.console.element.Align;
import dev.lumenlang.console.element.Element;
import dev.lumenlang.console.element.util.VisibleLength;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Horizontal sequence of children. Each child gets its preferred width and the row's full height.
 * Flexible children (width {@code -1}) split the leftover width evenly.
 */
public final class Row implements Element {

    private final @NotNull List<Element> children;
    private final int gap;
    private final @NotNull Align align;

    private Row(@NotNull List<Element> children, int gap, @NotNull Align align) {
        this.children = List.copyOf(children);
        this.gap = gap;
        this.align = align;
    }

    /**
     * Builds a row with the given children, no gap, start aligned.
     */
    public static @NotNull Row of(@NotNull Element @NotNull ... children) {
        return new Row(List.of(children), 0, Align.START);
    }

    /**
     * Returns a copy with a horizontal gap (spaces) between children.
     */
    public @NotNull Row gap(int columns) {
        return new Row(children, columns, align);
    }

    /**
     * Returns a copy with vertical alignment applied to each child within the row height.
     */
    public @NotNull Row align(@NotNull Align align) {
        return new Row(children, gap, align);
    }

    @Override
    public int width() {
        int total = 0;
        boolean any = false;
        for (Element c : children) {
            int w = c.width();
            if (w < 0) return -1;
            total += w;
            any = true;
        }
        return total + (any ? gap * (children.size() - 1) : 0);
    }

    @Override
    public int height() {
        int max = 0;
        boolean anyFlex = false;
        for (Element c : children) {
            int h = c.height();
            if (h < 0) anyFlex = true;
            else if (h > max) max = h;
        }
        return anyFlex ? -1 : max;
    }

    @Override
    public @NotNull String @NotNull [] render(int width, int height) {
        int fixedTotal = 0;
        int flexCount = 0;
        for (Element c : children) {
            int w = c.width();
            if (w < 0) flexCount++;
            else fixedTotal += w;
        }
        int gapTotal = gap * Math.max(0, children.size() - 1);
        int flexBudget = Math.max(0, width - fixedTotal - gapTotal);
        int flexEach = flexCount > 0 ? flexBudget / flexCount : 0;
        int flexExtra = flexCount > 0 ? flexBudget - flexEach * flexCount : 0;

        String[] out = new String[height];
        for (int row = 0; row < height; row++) out[row] = "";

        for (int i = 0; i < children.size(); i++) {
            Element c = children.get(i);
            int w = c.width();
            int assigned;
            if (w < 0) {
                assigned = flexEach + (flexExtra-- > 0 ? 1 : 0);
            } else {
                assigned = w;
            }
            int childHeight = c.height() < 0 ? height : Math.min(c.height(), height);
            String[] rendered = c.render(assigned, childHeight);
            for (int row = 0; row < height; row++) {
                String cell = alignCell(rendered, row, childHeight, height, assigned);
                out[row] = out[row] + cell;
                if (i < children.size() - 1 && gap > 0) out[row] = out[row] + " ".repeat(gap);
            }
        }

        for (int row = 0; row < height; row++) out[row] = VisibleLength.pad(out[row], width);
        return out;
    }

    private @NotNull String alignCell(@NotNull String @NotNull [] rendered, int row, int childHeight, int totalHeight, int assignedWidth) {
        int pad = totalHeight - childHeight;
        int offset = switch (align) {
            case START -> 0;
            case END -> pad;
            case CENTER -> pad / 2;
        };
        int idx = row - offset;
        if (idx < 0 || idx >= rendered.length) return " ".repeat(assignedWidth);
        return rendered[idx];
    }
}
