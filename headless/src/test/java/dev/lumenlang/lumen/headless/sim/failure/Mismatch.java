package dev.lumenlang.lumen.headless.sim.failure;

import org.jetbrains.annotations.NotNull;

/**
 * One typed disagreement between an expectation and an observed simulator output.
 */
public sealed interface Mismatch permits TopPatternMismatch, PrimaryIssueMismatch, AnyIssueMismatch, ConfidenceMismatch, SuggestionCountMismatch, SuggestionPresenceMismatch, ContainsPatternMismatch, CustomMismatch {

    /**
     * Short label naming the expectation that produced this mismatch.
     */
    @NotNull String label();
}
