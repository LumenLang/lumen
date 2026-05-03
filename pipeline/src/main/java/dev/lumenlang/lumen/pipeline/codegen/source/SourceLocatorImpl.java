package dev.lumenlang.lumen.pipeline.codegen.source;

import dev.lumenlang.lumen.api.codegen.source.SourceLocator;
import dev.lumenlang.lumen.api.codegen.source.SourceMap;
import dev.lumenlang.lumen.api.codegen.source.SourcePosition;
import dev.lumenlang.lumen.pipeline.language.nodes.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SourceLocatorImpl implements SourceLocator {

    private final Node node;
    private final List<Node> siblings;
    private final int index;
    private final SourceMap sourceMap;

    public SourceLocatorImpl(@NotNull Node node, @NotNull List<Node> siblings, int index, @NotNull SourceMap sourceMap) {
        this.node = node;
        this.siblings = siblings;
        this.index = index;
        this.sourceMap = sourceMap;
    }

    @Override
    public @NotNull SourcePosition current() {
        return new SourcePosition(node.line(), node.raw());
    }

    @Override
    public int currentLine() {
        return node.line();
    }

    @Override
    public @NotNull String currentRaw() {
        return node.raw();
    }

    @Override
    public @Nullable SourcePosition peekAhead(int offset) {
        int i = index + offset;
        if (i < 0 || i >= siblings.size()) return null;
        Node n = siblings.get(i);
        return new SourcePosition(n.line(), n.raw());
    }

    @Override
    public @Nullable SourcePosition peekBehind(int offset) {
        return peekAhead(-offset);
    }

    @Override
    public @NotNull SourcePosition requireAhead(int offset) {
        SourcePosition p = peekAhead(offset);
        if (p == null) throw new IndexOutOfBoundsException("no sibling at offset +" + offset);
        return p;
    }

    @Override
    public @NotNull SourcePosition requireBehind(int offset) {
        SourcePosition p = peekBehind(offset);
        if (p == null) throw new IndexOutOfBoundsException("no sibling at offset -" + offset);
        return p;
    }

    @Override
    public boolean hasAhead(int offset) {
        int i = index + offset;
        return i >= 0 && i < siblings.size();
    }

    @Override
    public boolean hasBehind(int offset) {
        return hasAhead(-offset);
    }

    @Override
    public boolean hasNext() {
        return index + 1 < siblings.size();
    }

    @Override
    public boolean hasPrev() {
        return index > 0;
    }

    @Override
    public boolean isFirst() {
        return index == 0;
    }

    @Override
    public boolean isLast() {
        return index + 1 >= siblings.size();
    }

    @Override
    public int siblingIndex() {
        return index;
    }

    @Override
    public int siblingCount() {
        return siblings.size();
    }

    @Override
    public @NotNull List<SourcePosition> siblings() {
        return mapNodes(siblings);
    }

    @Override
    public @NotNull List<SourcePosition> precedingSiblings() {
        return mapNodes(siblings.subList(0, index));
    }

    @Override
    public @NotNull List<SourcePosition> followingSiblings() {
        return mapNodes(siblings.subList(index + 1, siblings.size()));
    }

    @Override
    public @NotNull String rawAt(int line) {
        return sourceMap.rawAt(line);
    }

    @Override
    public @NotNull List<String> rawRange(int from, int to) {
        return sourceMap.rawRange(from, to);
    }

    @Override
    public boolean hasLine(int line) {
        return sourceMap.hasLine(line);
    }

    @Override
    public int lineCount() {
        return sourceMap.lineCount();
    }

    @Override
    public @NotNull String fullSource() {
        return sourceMap.fullSource();
    }

    private static @NotNull List<SourcePosition> mapNodes(@NotNull List<Node> ns) {
        List<SourcePosition> out = new ArrayList<>(ns.size());
        for (Node n : ns) out.add(new SourcePosition(n.line(), n.raw()));
        return Collections.unmodifiableList(out);
    }
}
