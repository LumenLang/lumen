package dev.lumenlang.lumen.headless.sim.failure;

import org.jetbrains.annotations.NotNull;

/**
 * Free-form failure produced by a user-supplied check passed to {@code SimulatorCase.expect(String, ...)}.
 *
 * @param description label supplied with the custom expectation
 * @param reason      message explaining why the check failed
 */
public record CustomMismatch(@NotNull String description, @NotNull String reason) implements Mismatch {

    @Override
    public @NotNull String label() {
        return description;
    }
}
