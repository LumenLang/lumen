package dev.lumenlang.lumen.pipeline.codegen.block;

import dev.lumenlang.lumen.api.codegen.block.BlockLocator;
import dev.lumenlang.lumen.api.codegen.source.BlockPosition;
import dev.lumenlang.lumen.pipeline.language.nodes.BlockNode;
import dev.lumenlang.lumen.pipeline.language.nodes.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BlockLocatorImpl implements BlockLocator {

    private final BlockNode current;
    private final List<BlockNode> blockSiblings;
    private final int blockIndex;

    public BlockLocatorImpl(@NotNull BlockNode current, @NotNull List<Node> allSiblings) {
        this.current = current;
        this.blockSiblings = new ArrayList<>();
        int idx = -1;
        for (Node n : allSiblings) {
            if (n instanceof BlockNode b) {
                if (b == current) idx = blockSiblings.size();
                blockSiblings.add(b);
            }
        }
        if (idx < 0) throw new IllegalArgumentException("current block not found among siblings");
        this.blockIndex = idx;
    }

    @Override
    public @NotNull BlockPosition current() {
        return toPosition(current);
    }

    @Override
    public int currentLine() {
        return current.line();
    }

    @Override
    public @NotNull String currentRaw() {
        return current.raw();
    }

    @Override
    public @NotNull String currentHeadToken() {
        return headToken(current);
    }

    @Override
    public @Nullable BlockPosition peekAhead(int offset) {
        int i = blockIndex + offset;
        if (i < 0 || i >= blockSiblings.size()) return null;
        return toPosition(blockSiblings.get(i));
    }

    @Override
    public @Nullable BlockPosition peekBehind(int offset) {
        return peekAhead(-offset);
    }

    @Override
    public @NotNull BlockPosition requireAhead(int offset) {
        BlockPosition p = peekAhead(offset);
        if (p == null) throw new IndexOutOfBoundsException("no sibling block at offset +" + offset);
        return p;
    }

    @Override
    public @NotNull BlockPosition requireBehind(int offset) {
        BlockPosition p = peekBehind(offset);
        if (p == null) throw new IndexOutOfBoundsException("no sibling block at offset -" + offset);
        return p;
    }

    @Override
    public boolean hasAhead(int offset) {
        int i = blockIndex + offset;
        return i >= 0 && i < blockSiblings.size();
    }

    @Override
    public boolean hasBehind(int offset) {
        return hasAhead(-offset);
    }

    @Override
    public boolean hasNext() {
        return blockIndex + 1 < blockSiblings.size();
    }

    @Override
    public boolean hasPrev() {
        return blockIndex > 0;
    }

    @Override
    public boolean isFirst() {
        return blockIndex == 0;
    }

    @Override
    public boolean isLast() {
        return blockIndex + 1 >= blockSiblings.size();
    }

    @Override
    public int blockIndex() {
        return blockIndex;
    }

    @Override
    public int blockCount() {
        return blockSiblings.size();
    }

    @Override
    public @NotNull List<BlockPosition> siblingBlocks() {
        return mapBlocks(blockSiblings);
    }

    @Override
    public @NotNull List<BlockPosition> precedingBlocks() {
        return mapBlocks(blockSiblings.subList(0, blockIndex));
    }

    @Override
    public @NotNull List<BlockPosition> followingBlocks() {
        return mapBlocks(blockSiblings.subList(blockIndex + 1, blockSiblings.size()));
    }

    @Override
    public @Nullable BlockPosition findPrecedingHead(@NotNull String literal) {
        for (int i = blockIndex - 1; i >= 0; i--) {
            if (literal.equalsIgnoreCase(headToken(blockSiblings.get(i)))) return toPosition(blockSiblings.get(i));
        }
        return null;
    }

    @Override
    public @Nullable BlockPosition findFollowingHead(@NotNull String literal) {
        for (int i = blockIndex + 1; i < blockSiblings.size(); i++) {
            if (literal.equalsIgnoreCase(headToken(blockSiblings.get(i)))) return toPosition(blockSiblings.get(i));
        }
        return null;
    }

    @Override
    public @Nullable BlockPosition findSiblingHead(@NotNull String literal) {
        BlockPosition p = findPrecedingHead(literal);
        return p != null ? p : findFollowingHead(literal);
    }

    @Override
    public boolean prevHeadEquals(@NotNull String literal) {
        BlockPosition p = peekBehind(1);
        return p != null && literal.equalsIgnoreCase(p.headToken());
    }

    @Override
    public boolean prevHeadExact(@NotNull String... tokens) {
        if (blockIndex == 0) return false;
        BlockNode prev = blockSiblings.get(blockIndex - 1);
        if (prev.head().size() != tokens.length) return false;
        for (int i = 0; i < tokens.length; i++) {
            if (!prev.head().get(i).text().equalsIgnoreCase(tokens[i])) return false;
        }
        return true;
    }

    private static @NotNull String headToken(@NotNull BlockNode b) {
        return b.head().isEmpty() ? "" : b.head().get(0).text().toLowerCase();
    }

    private static @NotNull BlockPosition toPosition(@NotNull BlockNode b) {
        return new BlockPosition(b.line(), b.raw(), headToken(b));
    }

    private static @NotNull List<BlockPosition> mapBlocks(@NotNull List<BlockNode> bs) {
        List<BlockPosition> out = new ArrayList<>(bs.size());
        for (BlockNode b : bs) out.add(toPosition(b));
        return Collections.unmodifiableList(out);
    }
}
