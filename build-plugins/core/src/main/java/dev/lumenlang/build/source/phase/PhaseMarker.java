package dev.lumenlang.build.source.phase;

import org.jetbrains.annotations.NotNull;

/**
 * A single {@code // lumen:compile} or {@code // lumen:runtime} marker found
 * in source.
 *
 * @param phase which phase the marker opens
 * @param line  1-based line number the marker sits on
 */
public record PhaseMarker(@NotNull Phase phase, int line) {
}
