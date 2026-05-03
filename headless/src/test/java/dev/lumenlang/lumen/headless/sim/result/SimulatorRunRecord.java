package dev.lumenlang.lumen.headless.sim.result;

import dev.lumenlang.lumen.headless.sim.cases.SimulatorCase;
import dev.lumenlang.lumen.headless.sim.failure.Mismatch;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator.Suggestion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One captured execution of a {@link SimulatorCase}.
 *
 * @param caseName        case display name
 * @param input           raw script fragment that was simulated
 * @param suggestions     suggestion list returned by the simulator
 * @param uncovered       coverage categories the case did not declare an expectation for
 * @param recommendations recommended {@code expect*} calls computed from the run
 * @param mismatches      typed mismatches produced by failing expectations, empty when the case passed
 */
public record SimulatorRunRecord(@NotNull String caseName, @NotNull String input, @NotNull List<Suggestion> suggestions,
                                 @NotNull List<String> uncovered, @NotNull List<String> recommendations,
                                 @NotNull List<Mismatch> mismatches) {

    /**
     * {@code true} when no mismatches were produced.
     */
    public boolean passed() {
        return mismatches.isEmpty();
    }

    /**
     * Highest-confidence suggestion, or {@code null} when none was returned.
     */
    public @Nullable Suggestion top() {
        return suggestions.isEmpty() ? null : suggestions.get(0);
    }
}
