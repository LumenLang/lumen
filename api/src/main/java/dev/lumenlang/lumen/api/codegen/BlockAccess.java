package dev.lumenlang.lumen.api.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides contextual information about the current block's position in the AST.
 *
 * <p>Handlers use this to inspect sibling blocks, check scope depth, and enforce structural
 * constraints (e.g. ensuring an {@code else} follows an {@code if}).
 *
 * @see HandlerContext#block()
 */
@SuppressWarnings("unused")
public interface BlockAccess {

    /**
     * Returns {@code true} if this block has no enclosing parent block (i.e. it is at the
     * top level of the script).
     *
     * @return {@code true} if this is a root-level block
     */
    boolean isRoot();

    /**
     * Returns {@code true} if this is the first node among its siblings.
     *
     * @return {@code true} if there is no preceding sibling
     */
    boolean isFirst();

    /**
     * Returns {@code true} if the preceding sibling is a block whose first head token matches
     * the given literal (case-insensitively).
     *
     * <p><strong>Warning:</strong> this only checks the <em>first</em> token. Use
     * {@link #prevHeadExact(String...)} when you need to match the entire head.
     *
     * @param literal the text to match against the first head token
     * @return {@code true} if it matches
     */
    boolean prevHeadEquals(@NotNull String literal);

    /**
     * Returns {@code true} if the preceding sibling is a block whose head matches
     * the supplied tokens exactly  -  same count and each matching case-insensitively.
     *
     * @param tokens the expected head tokens in order
     * @return {@code true} if the previous sibling's head matches exactly
     */
    boolean prevHeadExact(@NotNull String... tokens);

    /**
     * Returns the source line number of the preceding sibling node, or {@code -1} if
     * this is the first sibling.
     *
     * @return the 1-based line number of the previous sibling, or {@code -1}
     */
    int prevLine();

    /**
     * Returns the raw source text of the preceding sibling node, or an empty string
     * if this is the first sibling.
     *
     * @return the raw source of the previous sibling
     */
    @NotNull String prevRaw();

    /**
     * Returns the source line number of the AST node for this block.
     *
     * @return the 1-based line number
     */
    int line();

    /**
     * Returns the raw source text of the line that opened this block.
     *
     * @return the raw source line
     */
    @NotNull String raw();

    /**
     * Stores a value in this block's local environment map.
     *
     * <p>Child blocks can later retrieve this value via {@link #getEnvFromParents(String)}
     * or {@link #getEnvUpTo(String, int)}.
     *
     * @param key   the key
     * @param value the value
     */
    void putEnv(@NotNull String key, @NotNull Object value);

    /**
     * Retrieves a value from this block's local environment only (no parent walk).
     *
     * @param key the key
     * @return the value, or {@code null} if not present in this frame
     */
    @Nullable <T> T getEnv(@NotNull String key);

    /**
     * Retrieves a value by walking the scope stack from this block up through all
     * parent blocks, returning the first match.
     *
     * @param key the key
     * @return the value from the nearest enclosing scope, or {@code null}
     */
    @Nullable <T> T getEnvFromParents(@NotNull String key);

    /**
     * Retrieves a value by walking the scope stack up to {@code maxDepth} parent levels.
     *
     * <p>A {@code maxDepth} of 0 searches only this block. A value of 1 searches this
     * block and its direct parent, and so on.
     *
     * @param key      the key
     * @param maxDepth the maximum number of parent levels to walk (0 = this block only)
     * @return the value from the nearest matching scope within the depth limit, or {@code null}
     */
    @Nullable <T> T getEnvUpTo(@NotNull String key, int maxDepth);

    /**
     * Returns {@code true} if this node has a following sibling at the same indentation level.
     *
     * @return {@code true} if there is a next sibling
     */
    boolean hasNext();

    /**
     * Returns {@code true} if this is the last node among its siblings.
     *
     * @return {@code true} if there is no following sibling
     */
    boolean isLast();

    /**
     * Returns the source line number of the next sibling node, or {@code -1} if
     * this is the last sibling.
     *
     * @return the 1-based line number of the next sibling, or {@code -1}
     */
    int nextLine();

    /**
     * Returns the raw source text of the next sibling node, or an empty string
     * if this is the last sibling.
     *
     * @return the raw source of the next sibling
     */
    @NotNull String nextRaw();

    /**
     * Searches all preceding sibling blocks for one whose first head token matches the given
     * literal (case-insensitively). Returns a snapshot of the match, or {@code null} if none found.
     *
     * <p>Searches backwards from the sibling immediately before this block.
     *
     * @param literal the first token to match (e.g. {@code "if"})
     * @return the matching sibling info, or {@code null}
     */
    @Nullable SiblingInfo findPrecedingBlock(@NotNull String literal);

    /**
     * Searches all sibling blocks (both before and after this block) for one whose first head
     * token matches the given literal (case-insensitively). Returns a snapshot of the nearest
     * match, or {@code null} if none found.
     *
     * @param literal the first token to match (e.g. {@code "if"})
     * @return the matching sibling info, or {@code null}
     */
    @Nullable SiblingInfo findSiblingBlock(@NotNull String literal);

    /**
     * A snapshot of a sibling block's location and source text.
     *
     * @param line the 1-based source line number
     * @param raw  the raw source text of the block header
     */
    record SiblingInfo(int line, @NotNull String raw) {
    }
}
