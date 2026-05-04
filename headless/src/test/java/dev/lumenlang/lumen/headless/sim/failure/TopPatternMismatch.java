package dev.lumenlang.lumen.headless.sim.failure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Top suggestion's pattern did not equal the expected value.
 *
 * @param expected pattern raw text the case expected as the top suggestion
 * @param actual   pattern raw text actually returned, or {@code null} when no suggestion was produced
 */
public record TopPatternMismatch(@NotNull String expected, @Nullable String actual) implements Mismatch {

    @Override
    public @NotNull String label() {
        return "top pattern";
    }
}
