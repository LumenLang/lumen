package dev.lumenlang.lumen.api.codegen.source;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Read-only lookup of script source by line.
 */
public interface SourceMap {

    /**
     * Raw source text of the given 1-based line.
     *
     * @throws IndexOutOfBoundsException when {@code line} is outside {@code [1, lineCount()]}
     */
    @NotNull String rawAt(int line);

    /**
     * Raw source text for the inclusive line range {@code [from, to]}.
     *
     * @throws IndexOutOfBoundsException when either endpoint is outside the script
     * @throws IllegalArgumentException  when {@code from > to}
     */
    @NotNull List<String> rawRange(int from, int to);

    /**
     * {@code true} when {@code line} is within {@code [1, lineCount()]}.
     */
    boolean hasLine(int line);

    /**
     * Total number of lines in the script.
     */
    int lineCount();

    /**
     * Full source text of the script.
     */
    @NotNull String fullSource();
}
