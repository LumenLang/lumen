package dev.lumenlang.build.result;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Outcome of a single build pipeline run.
 *
 * @param rewrittenClasses     count of {@code .class} files updated on disk
 * @param sourceEntriesEmitted count of handler source-text entries written to the sidecar
 * @param diagnostics          all errors and warnings collected during the run
 */
public record BuildResult(int rewrittenClasses, int sourceEntriesEmitted, @NotNull List<Diagnostic> diagnostics) {
}
