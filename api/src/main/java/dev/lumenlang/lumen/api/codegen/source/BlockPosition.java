package dev.lumenlang.lumen.api.codegen.source;

import org.jetbrains.annotations.NotNull;

/**
 * Source location and identity of a block.
 *
 * @param line      1-based line number of the block's head
 * @param raw       raw source text of the block's head line
 * @param headToken first token text of the block's head, lowercased
 */
public record BlockPosition(int line, @NotNull String raw, @NotNull String headToken) {
}
