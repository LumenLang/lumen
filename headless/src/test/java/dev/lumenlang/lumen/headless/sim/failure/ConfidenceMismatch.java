package dev.lumenlang.lumen.headless.sim.failure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Top suggestion's confidence fell below the expected minimum.
 *
 * @param minimum required minimum confidence in {@code [0, 1]}
 * @param actual  observed top confidence, or {@code null} when no suggestion was produced
 */
public record ConfidenceMismatch(double minimum, @Nullable Double actual) implements Mismatch {

    @Override
    public @NotNull String label() {
        return "confidence";
    }
}
