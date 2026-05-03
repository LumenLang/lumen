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
    DebugSink STDERR = (depth, line) -> {
        StringBuilder sb = new StringBuilder(depth * 2 + line.length());
        for (int i = 0; i < depth; i++) sb.append("  ");
        sb.append(line);
        System.err.println(sb);
    };

    /**
     * Emits one debug line at the given indent depth.
     */
    void write(int depth, @NotNull String line);
}
