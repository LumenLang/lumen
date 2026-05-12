package dev.lumenlang.lumen.pipeline.language.suggestor.result;

import dev.lumenlang.lumen.api.language.SemanticKind;
import org.jetbrains.annotations.NotNull;

/**
 * One completion offered to the editor's popup list.
 *
 * @param insertText   text to insert on accept
 * @param displayLabel label shown in the popup
 * @param detail       short type or category hint
 * @param kind         editor category for icon selection
 * @param replaceFrom  source column to replace from (inclusive)
 * @param replaceTo    source column to replace to (exclusive)
 * @param score        editor-side rank score (0..1)
 */
public record CompletionItem(@NotNull String insertText, @NotNull String displayLabel,
                             @NotNull String detail, @NotNull SemanticKind kind,
                             int replaceFrom, int replaceTo, double score) {
}
