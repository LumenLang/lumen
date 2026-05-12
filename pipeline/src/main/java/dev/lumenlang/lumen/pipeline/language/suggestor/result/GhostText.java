package dev.lumenlang.lumen.pipeline.language.suggestor.result;

import org.jetbrains.annotations.NotNull;

/**
 * Inline ghost-text continuation for AI-style preview.
 *
 * @param text     full text to render after the cursor
 * @param insertAt source column where the ghost starts
 */
public record GhostText(@NotNull String text, int insertAt) {
}
