package dev.lumenlang.lumen.pipeline.inject;

import org.jetbrains.annotations.NotNull;

/**
 * Implemented by injectable handlers to receive the pattern string they are registered with.
 * Allows getting meaningful method names and validating binding names early.
 */
public interface PatternHinted {

    /**
     * Provides the pattern string this handler is registered with.
     *
     * @param pattern the raw pattern string
     */
    void patternHint(@NotNull String pattern);
}
