package dev.lumenlang.lumen.headless.sim.snapshot;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Captured output of one simulator run, ready to be diffed against a stored baseline.
 *
 * @param caseName    display name from the {@code @SimCase} annotation
 * @param input       raw input fragment fed to the simulator
 * @param runner      runner enum name (STATEMENT, EXPRESSION, CONDITION, BLOCK)
 * @param env         human-readable env summary lines, e.g. {@code "var p: PLAYER"}
 * @param suggestions ordered ranking emitted by the simulator
 */
public record Snapshot(@NotNull String caseName, @NotNull String input, @NotNull String runner,
                       @NotNull List<String> env, @NotNull List<SuggestionSnap> suggestions) {
}
