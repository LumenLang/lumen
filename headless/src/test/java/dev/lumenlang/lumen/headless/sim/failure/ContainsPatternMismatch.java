package dev.lumenlang.lumen.headless.sim.failure;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * No suggestion in the returned list matched the expected pattern raw.
 *
 * @param expected pattern raw text the case expected somewhere in the list
 * @param actual   pattern raws actually returned, in confidence order
 */
public record ContainsPatternMismatch(@NotNull String expected, @NotNull List<String> actual) implements Mismatch {

    @Override
    public @NotNull String label() {
        return "contains pattern";
    }
}
