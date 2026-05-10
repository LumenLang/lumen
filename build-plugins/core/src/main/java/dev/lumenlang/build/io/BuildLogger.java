package dev.lumenlang.build.io;

import org.jetbrains.annotations.NotNull;

/**
 * Build-system-agnostic log sink. Each shim adapts to its native logger.
 */
public interface BuildLogger {

    void info(@NotNull String message);

    void warn(@NotNull String message);

    void error(@NotNull String message);
}
