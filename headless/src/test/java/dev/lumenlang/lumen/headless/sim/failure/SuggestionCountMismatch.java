package dev.lumenlang.lumen.headless.sim.failure;

import org.jetbrains.annotations.NotNull;

/**
 * Number of suggestions returned was outside the expected closed range.
 *
 * @param min    minimum acceptable count
 * @param max    maximum acceptable count
 * @param actual observed count
 */
public record SuggestionCountMismatch(int min, int max, int actual) implements Mismatch {

    @Override
    public @NotNull String label() {
        return "suggestion count";
    }
}
