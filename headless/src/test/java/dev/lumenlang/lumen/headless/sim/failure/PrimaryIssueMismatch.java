package dev.lumenlang.lumen.headless.sim.failure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Top suggestion's primary issue did not match the expected type or value-level check.
 *
 * @param expectedType simple class name of the expected primary issue
 * @param actualTypes  simple class names of the issues actually attached, in order, or empty when none
 * @param customReason failure detail produced by a value-level check, or {@code null} when not applicable
 */
public record PrimaryIssueMismatch(@NotNull String expectedType, @NotNull List<String> actualTypes,
                                   @Nullable String customReason) implements Mismatch {

    @Override
    public @NotNull String label() {
        return "primary issue";
    }
}
