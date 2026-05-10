package dev.lumenlang.lumen.plugin.scripts.model.compiled;

/**
 * Parse and compile timings for a single script load.
 *
 * @param parseNanos   nanoseconds spent parsing, zero on cache hit
 * @param compileNanos nanoseconds spent compiling
 */
public record CompileTimings(long parseNanos, long compileNanos) {
}
