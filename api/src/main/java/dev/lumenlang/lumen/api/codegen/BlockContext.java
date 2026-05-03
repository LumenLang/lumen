package dev.lumenlang.lumen.api.codegen;

import dev.lumenlang.lumen.api.codegen.block.BlockLocator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A block scope frame in the script being compiled.
 */
@SuppressWarnings("unused")
public interface BlockContext {

    /**
     * Block-scoped position lookups for the current block.
     */
    @NotNull BlockLocator locator();

    /**
     * Parent block scope, or {@code null} when this is the root block.
     */
    @Nullable BlockContext parent();

    /**
     * {@code true} when this block has no parent.
     */
    boolean isRoot();

    /**
     * Number of parent blocks above this one. Root has depth {@code 0}.
     */
    int depth();

    /**
     * Stores a value in this block's local environment map.
     */
    void putEnv(@NotNull String key, @NotNull Object value);

    /**
     * Retrieves a value from this block's local environment only.
     */
    @Nullable <T> T getEnv(@NotNull String key);

    /**
     * Retrieves a value by walking from this block up through all parent blocks, returning the first match.
     */
    @Nullable <T> T getEnvFromParents(@NotNull String key);

    /**
     * Retrieves a value by walking up to {@code maxDepth} parent levels.
     *
     * @param maxDepth maximum number of parent levels to walk, where {@code 0} searches only this block
     */
    @Nullable <T> T getEnvUpTo(@NotNull String key, int maxDepth);
}
