package dev.lumenlang.build.result;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * A single build-time problem report.
 *
 * @param severity error or warning
 * @param message  human-readable description
 * @param file     source file the problem refers to, or null when not file-bound
 * @param line     1-based line, or 0 when unknown
 * @param column   1-based column, or 0 when unknown
 */
public record Diagnostic(@NotNull Severity severity, @NotNull String message, @Nullable Path file, int line, int column) {
}
