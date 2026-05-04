package dev.lumenlang.lumen.pipeline.language.simulator.debug;

import org.jetbrains.annotations.NotNull;

/**
 * Receives formatted debug lines from the pattern simulator. Indent depth carries stage nesting.
 */
@FunctionalInterface
public interface DebugSink {

    /**
     * Discards every line.
     */
    DebugSink NOOP = (depth, line) -> {
    };

    /**
     * Writes lines to {@code System.err} prefixed with {@code "  "} per indent level.
     */
    DebugSink STDERR = (depth, line) -> System.err.println("  ".repeat(depth) + line);

    /**
     * Emits one debug line at the given indent depth.
     */
    void write(int depth, @NotNull String line);
}
