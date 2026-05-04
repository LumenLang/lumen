package dev.lumenlang.lumen.headless.sim.failure;

import org.jetbrains.annotations.NotNull;

/**
 * Suggestion presence did not match the expectation.
 *
 * @param expectedNonEmpty {@code true} when the case expected at least one suggestion
 * @param actualCount      number of suggestions observed
 */
public record SuggestionPresenceMismatch(boolean expectedNonEmpty, int actualCount) implements Mismatch {

    @Override
    public @NotNull String label() {
        return expectedNonEmpty ? "expected suggestions" : "expected no suggestions";
    }
}
