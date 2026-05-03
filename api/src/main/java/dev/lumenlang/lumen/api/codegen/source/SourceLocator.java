package dev.lumenlang.lumen.api.codegen.source;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Source-position lookups bound to a current node.
 */
public interface SourceLocator extends SourceMap {

    /**
     * Position of the current node.
     */
    @NotNull SourcePosition current();

    /**
     * 1-based line number of the current node.
     */
    int currentLine();

    /**
     * Raw source text of the current node's line.
     */
    @NotNull String currentRaw();

    /**
     * Sibling at {@code offset} positions after the current node within the same block,
     * or {@code null} when the offset exits the block.
     *
     * @param offset positive distance, where {@code 1} is the immediate next sibling
     */
    @Nullable SourcePosition peekAhead(int offset);

    /**
     * Sibling at {@code offset} positions before the current node within the same block,
     * or {@code null} when the offset exits the block.
     *
     * @param offset positive distance, where {@code 1} is the immediate previous sibling
     */
    @Nullable SourcePosition peekBehind(int offset);

    /**
     * Sibling at {@code offset} positions after the current node within the same block.
     *
     * @throws IndexOutOfBoundsException when no sibling exists at that offset
     */
    @NotNull SourcePosition requireAhead(int offset);

    /**
     * Sibling at {@code offset} positions before the current node within the same block.
     *
     * @throws IndexOutOfBoundsException when no sibling exists at that offset
     */
    @NotNull SourcePosition requireBehind(int offset);

    /**
     * {@code true} when a sibling exists {@code offset} positions ahead within the same block.
     */
    boolean hasAhead(int offset);

    /**
     * {@code true} when a sibling exists {@code offset} positions behind within the same block.
     */
    boolean hasBehind(int offset);

    /**
     * {@code true} when at least one sibling follows the current node in the same block.
     */
    boolean hasNext();

    /**
     * {@code true} when at least one sibling precedes the current node in the same block.
     */
    boolean hasPrev();

    /**
     * {@code true} when the current node is the first sibling in its block.
     */
    boolean isFirst();

    /**
     * {@code true} when the current node is the last sibling in its block.
     */
    boolean isLast();

    /**
     * Index of the current node among its siblings, 0-based.
     */
    int siblingIndex();

    /**
     * Total sibling count in the current node's block.
     */
    int siblingCount();

    /**
     * All sibling positions in declaration order, including the current node.
     */
    @NotNull List<SourcePosition> siblings();

    /**
     * Sibling positions that precede the current node, in declaration order.
     */
    @NotNull List<SourcePosition> precedingSiblings();

    /**
     * Sibling positions that follow the current node, in declaration order.
     */
    @NotNull List<SourcePosition> followingSiblings();
}
