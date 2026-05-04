package dev.lumenlang.lumen.headless.sim.failure;

import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator.SuggestionIssue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Top suggestion was expected to match the pattern cleanly: equal raw, zero issues, full
 * confidence. The supplied fields describe whichever invariant failed.
 *
 * @param expectedPattern pattern raw text expected as the top match
 * @param actualPattern   pattern raw text actually ranked first, or {@code null} when none returned
 * @param actualIssues    issues attached to the actual top suggestion
 * @param actualConfidence confidence of the actual top suggestion, or {@code null} when none returned
 */
public record CleanTopMismatch(@NotNull String expectedPattern, @Nullable String actualPattern,
                               @NotNull List<String> actualIssues,
                               @Nullable Double actualConfidence) implements Mismatch {

    @Override
    public @NotNull String label() {
        return "clean top";
    }

    /**
     * Builds a list of class-name strings for {@code issues}.
     */
    public static @NotNull List<String> issueNames(@NotNull List<SuggestionIssue> issues) {
        return issues.stream().map(i -> i.getClass().getSimpleName()).toList();
    }
}
