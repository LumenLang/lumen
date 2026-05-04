package dev.lumenlang.lumen.headless.sim.failure;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Top suggestion's issue list did not contain any issue of the expected type.
 *
 * @param expectedType simple class name of the issue type that should have been present
 * @param actualTypes  simple class names of the issues actually attached, in order
 */
public record AnyIssueMismatch(@NotNull String expectedType, @NotNull List<String> actualTypes) implements Mismatch {

    @Override
    public @NotNull String label() {
        return "issue presence";
    }
}
