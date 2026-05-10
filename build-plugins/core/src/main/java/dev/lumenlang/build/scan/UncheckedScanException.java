package dev.lumenlang.build.scan;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Wraps an {@link IOException} raised inside a {@link java.util.stream.Stream}
 * walk so the scanner can surface the original cause to the caller.
 */
public final class UncheckedScanException extends RuntimeException {

    private final IOException ioCause;

    public UncheckedScanException(@NotNull Path path, @NotNull IOException cause) {
        super("Failed to scan " + path, cause);
        this.ioCause = cause;
    }

    public @NotNull IOException ioCause() {
        return ioCause;
    }
}
