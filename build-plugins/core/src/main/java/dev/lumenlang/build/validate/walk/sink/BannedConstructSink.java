package dev.lumenlang.build.validate.walk.sink;

import org.jetbrains.annotations.NotNull;

/**
 * Receives reports of constructs the walker cannot precisely analyze, e.g.
 * anonymous class bodies inside handler bodies.
 */
@FunctionalInterface
public interface BannedConstructSink {

    void accept(@NotNull String construct, int line);
}
