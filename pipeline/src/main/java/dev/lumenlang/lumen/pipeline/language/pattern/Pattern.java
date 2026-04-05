package dev.lumenlang.lumen.pipeline.language.pattern;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A compiled representation of a Lumen pattern string.
 *
 * @param raw   the original, unmodified pattern string
 * @param parts the ordered list of literal and placeholders parts
 */
public record Pattern(@NotNull String raw, @NotNull List<PatternPart> parts) {
}