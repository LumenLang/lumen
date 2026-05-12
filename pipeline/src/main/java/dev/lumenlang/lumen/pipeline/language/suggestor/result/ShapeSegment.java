package dev.lumenlang.lumen.pipeline.language.suggestor.result;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * One segment of a pattern's rendered shape.
 *
 * @param kind      whether the segment is a literal or placeholder, matched or pending
 * @param text      display text (literal form, or {@code %name:TYPE%} for a placeholder)
 * @param bindingId type binding id when {@link #kind} is a placeholder, otherwise {@code null}
 */
public record ShapeSegment(@NotNull SegmentKind kind, @NotNull String text, @Nullable String bindingId) {
}
