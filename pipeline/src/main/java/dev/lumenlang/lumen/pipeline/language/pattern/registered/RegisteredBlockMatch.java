package dev.lumenlang.lumen.pipeline.language.pattern.registered;

import dev.lumenlang.lumen.pipeline.language.match.Match;
import org.jetbrains.annotations.NotNull;

/**
 * The result of a successful block pattern match, bundling the matched
 * {@link RegisteredBlock} with its corresponding {@link Match}.
 *
 * @param reg   the registered block that was matched
 * @param match the match result containing the bound parameter values
 */
public record RegisteredBlockMatch(@NotNull RegisteredBlock reg, @NotNull Match match) {
}
