package dev.lumenlang.lumen.pipeline.language.simulator.prefilter.result;

import org.jetbrains.annotations.NotNull;

/**
 * Outcome of matching one {@link LiteralInfo} against the input tokens during pre-filtering.
 *
 * @param literal    the literal that was matched
 * @param tokenIndex input token index that satisfied the literal, or {@code -1} when the literal
 *                   was not found
 * @param distance   fuzzy edit distance between token and literal form
 */
public record LiteralMatchResult(@NotNull LiteralInfo literal, int tokenIndex, int distance) {
}
