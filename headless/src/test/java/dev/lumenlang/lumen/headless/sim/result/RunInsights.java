package dev.lumenlang.lumen.headless.sim.result;

import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator.Suggestion;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator.SuggestionIssue;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Computes recommended {@code expect*} calls for a {@link PatternSimulator} run based on the
 * coverage categories the case did not declare an expectation for.
 */
public final class RunInsights {

    private RunInsights() {
    }

    /**
     * Recommendations for {@code uncovered} categories, anchored to {@code suggestions}.
     */
    public static @NotNull List<String> recommendations(@NotNull List<Suggestion> suggestions, @NotNull Set<String> uncovered) {
        List<String> out = new ArrayList<>();
        if (suggestions.isEmpty()) return out;
        Suggestion top = suggestions.get(0);
        if (uncovered.contains("TOP_PATTERN")) {
            out.add(".expectTopPattern(\"" + top.pattern().raw() + "\")");
        }
        if (uncovered.contains("PRIMARY_ISSUE") && !top.issues().isEmpty()) {
            SuggestionIssue primary = top.issues().get(0);
            out.add(".expectPrimaryIssue(SuggestionIssue." + primary.getClass().getSimpleName() + ".class)");
        }
        if (uncovered.contains("ANY_ISSUE") && !top.issues().isEmpty()) {
            for (SuggestionIssue issue : top.issues()) {
                out.add(".expectAnyIssue(SuggestionIssue." + issue.getClass().getSimpleName() + ".class)");
            }
        }
        if (uncovered.contains("CONFIDENCE_MIN")) {
            out.add(".expectConfidenceAtLeast(" + round3(top.confidence()) + ")");
        }
        if (uncovered.contains("SUGGESTION_COUNT")) {
            out.add(".expectSuggestionCount(" + suggestions.size() + ", " + suggestions.size() + ")");
        }
        return out;
    }

    private static double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
