package dev.lumenlang.console.element.impl.widget;

import dev.lumenlang.console.element.Element;
import dev.lumenlang.console.element.impl.basic.Text;
import dev.lumenlang.console.element.layout.Column;
import dev.lumenlang.console.element.layout.Row;
import dev.lumenlang.console.style.Color;
import dev.lumenlang.console.style.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Composable tree view. Each node has a label element and optional children. Renders ASCII tree
 * connectors automatically based on position in the parent.
 */
public final class Tree implements Element {

    private final @Nullable Element title;
    private final @NotNull List<Node> nodes;
    private final @NotNull Style connectorStyle;

    private Tree(@Nullable Element title, @NotNull List<Node> nodes, @NotNull Style connectorStyle) {
        this.title = title;
        this.nodes = List.copyOf(nodes);
        this.connectorStyle = connectorStyle;
    }

    /**
     * Builds a tree with no title and the given top-level nodes.
     */
    public static @NotNull Tree of(@NotNull Node @NotNull ... nodes) {
        return new Tree(null, List.of(nodes), Style.fg(Color.SLATE));
    }

    /**
     * Returns a copy with a header element rendered above the tree.
     */
    public @NotNull Tree title(@NotNull Element title) {
        return new Tree(title, nodes, connectorStyle);
    }

    /**
     * Returns a copy with the connector glyph style replaced.
     */
    public @NotNull Tree connectorStyle(@NotNull Style style) {
        return new Tree(title, nodes, style);
    }

    private @NotNull Element compose() {
        List<Element> rows = new ArrayList<>();
        if (title != null) rows.add(title);
        for (int i = 0; i < nodes.size(); i++) {
            boolean last = i == nodes.size() - 1;
            appendNode(rows, nodes.get(i), "", last);
        }
        return Column.of(rows.toArray(new Element[0]));
    }

    private void appendNode(@NotNull List<Element> rows, @NotNull Node node, @NotNull String prefix, boolean last) {
        String connector = last ? "└─ " : "├─ ";
        Element row = Row.of(Text.of(prefix + connector).style(connectorStyle), node.label);
        rows.add(row);
        for (Element extra : node.detail) {
            String detailPrefix = prefix + (last ? "   " : "│  ");
            rows.add(Row.of(Text.of(detailPrefix + "   ").style(connectorStyle), extra));
        }
        for (int i = 0; i < node.children.size(); i++) {
            boolean childLast = i == node.children.size() - 1;
            String childPrefix = prefix + (last ? "   " : "│  ");
            appendNode(rows, node.children.get(i), childPrefix, childLast);
        }
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

    /**
     * Single node in a tree. Construct via {@link #leaf}, {@link #branch}, or
     * {@link #withDetail}.
     */
    public static final class Node {
        private final @NotNull Element label;
        private final @NotNull List<Node> children;
        private final @NotNull List<Element> detail;

        private Node(@NotNull Element label, @NotNull List<Node> children, @NotNull List<Element> detail) {
            this.label = label;
            this.children = List.copyOf(children);
            this.detail = List.copyOf(detail);
        }

        /**
         * Builds a leaf node with no children.
         */
        public static @NotNull Node leaf(@NotNull Element label) {
            return new Node(label, List.of(), List.of());
        }

        /**
         * Builds a node with children rendered indented below.
         */
        public static @NotNull Node branch(@NotNull Element label, @NotNull Node @NotNull ... children) {
            return new Node(label, List.of(children), List.of());
        }

        /**
         * Returns a copy with the given detail rows added below the label, indented under it.
         */
        public @NotNull Node withDetail(@NotNull Element @NotNull ... detail) {
            return new Node(label, children, List.of(detail));
        }
    }
}
