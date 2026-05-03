package dev.lumenlang.lumen.api.codegen.block;

import dev.lumenlang.lumen.api.codegen.source.BlockPosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Block-scoped position lookups that ignore non-block siblings.
 */
public interface BlockLocator {

    /**
     * Position of the current block.
     */
    @NotNull BlockPosition current();

    /**
     * 1-based line number of the current block's head.
     */
    int currentLine();

    /**
     * Raw source text of the current block's head line.
     */
    @NotNull String currentRaw();

    /**
     * First head token of the current block, lowercased.
     */
    @NotNull String currentHeadToken();

    /**
     * Sibling block at {@code offset} positions after the current block, skipping non-block siblings,
     * or {@code null} when the offset exits the parent block.
     *
     * @param offset positive distance, where {@code 1} is the immediate next sibling block
     */
    @Nullable BlockPosition peekAhead(int offset);

    /**
     * Sibling block at {@code offset} positions before the current block, skipping non-block siblings,
     * or {@code null} when the offset exits the parent block.
     *
     * @param offset positive distance, where {@code 1} is the immediate previous sibling block
     */
    @Nullable BlockPosition peekBehind(int offset);

    /**
     * Sibling block at {@code offset} positions after the current block.
     *
     * @throws IndexOutOfBoundsException when no sibling block exists at that offset
     */
    @NotNull BlockPosition requireAhead(int offset);

    /**
     * Sibling block at {@code offset} positions before the current block.
     *
     * @throws IndexOutOfBoundsException when no sibling block exists at that offset
     */
    @NotNull BlockPosition requireBehind(int offset);

    /**
     * {@code true} when a sibling block exists {@code offset} positions ahead.
     */
    boolean hasAhead(int offset);

    /**
     * {@code true} when a sibling block exists {@code offset} positions behind.
     */
    boolean hasBehind(int offset);

    /**
     * {@code true} when at least one sibling block follows the current block.
     */
    boolean hasNext();

    /**
     * {@code true} when at least one sibling block precedes the current block.
     */
    boolean hasPrev();

    /**
     * {@code true} when the current block is the first sibling block in its parent.
     */
    boolean isFirst();

    /**
     * {@code true} when the current block is the last sibling block in its parent.
     */
    boolean isLast();

    /**
     * Index of the current block among its sibling blocks, 0-based.
     */
    int blockIndex();

    /**
     * Total sibling block count in the parent.
     */
    int blockCount();

    /**
     * All sibling block positions in declaration order, including the current block.
     */
    @NotNull List<BlockPosition> siblingBlocks();

    /**
     * Sibling block positions that precede the current block, in declaration order.
     */
    @NotNull List<BlockPosition> precedingBlocks();

    /**
     * Sibling block positions that follow the current block, in declaration order.
     */
    @NotNull List<BlockPosition> followingBlocks();

    /**
     * Nearest preceding sibling block whose first head token matches {@code literal} case-insensitively,
     * or {@code null} when none matches.
     */
    @Nullable BlockPosition findPrecedingHead(@NotNull String literal);

    /**
     * Nearest following sibling block whose first head token matches {@code literal} case-insensitively,
     * or {@code null} when none matches.
     */
    @Nullable BlockPosition findFollowingHead(@NotNull String literal);

    /**
     * Nearest sibling block (preceding then following) whose first head token matches {@code literal}
     * case-insensitively, or {@code null} when none matches.
     */
    @Nullable BlockPosition findSiblingHead(@NotNull String literal);

    /**
     * {@code true} when the immediately preceding sibling block exists and its first head token equals
     * {@code literal} case-insensitively.
     */
    boolean prevHeadEquals(@NotNull String literal);

    /**
     * {@code true} when the immediately preceding sibling block exists and its head matches the supplied
     * tokens exactly, same count and each comparison case-insensitive.
     */
    boolean prevHeadExact(@NotNull String... tokens);
}
