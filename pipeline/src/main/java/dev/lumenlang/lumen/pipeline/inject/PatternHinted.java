package dev.lumenlang.lumen.pipeline.inject;

import org.jetbrains.annotations.NotNull;

/**
 * Implemented by injectable handlers to receive the pattern string they are registered with.
 * Allows getting meaningful method names and validating binding names early.
 */
public interface PatternHinted {

    /**
     * Provides the pattern string this handler is registered with.
     * Sets the generated method name and validates binding names.
     *
     * @param pattern the raw pattern string
     */
    void patternHint(@NotNull String pattern);

    /**
     * Validates that the binding names in an additional pattern are compatible
     * with this handler's extracted bindings, without changing the method name.
     *
     * @param pattern the additional raw pattern string to validate
     */
    default void validateAdditionalPattern(@NotNull String pattern) {
    }
}
