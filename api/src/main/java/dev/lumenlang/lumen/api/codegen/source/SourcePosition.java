package dev.lumenlang.lumen.api.codegen.source;

import org.jetbrains.annotations.NotNull;

/**
 * A single point in the script source.
 *
 * @param line 1-based line number
 * @param raw  raw source text of the line
 */
public record SourcePosition(int line, @NotNull String raw) {
}
