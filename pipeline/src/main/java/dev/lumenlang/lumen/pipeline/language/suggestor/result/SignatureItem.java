package dev.lumenlang.lumen.pipeline.language.suggestor.result;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Pattern shape rendered for the "above/below" line.
 *
 * @param renderedShape      joined display text of every segment
 * @param segments           per-part segments in declaration order
 * @param activeSegmentIndex index of the segment the cursor is in, or {@code -1} when the
 *                           pattern is fully matched and the cursor is past the last segment
 */
public record SignatureItem(@NotNull String renderedShape, @NotNull List<ShapeSegment> segments,
                            int activeSegmentIndex) {
}
