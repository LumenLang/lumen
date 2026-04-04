package dev.lumenlang.lumen.pipeline.language.pattern.registered;

import dev.lumenlang.lumen.pipeline.language.match.Match;
import org.jetbrains.annotations.NotNull;

/**
 * The result of a successful statement pattern match, bundling the matched
 * {@link RegisteredPattern} with its corresponding {@link Match}.
 *
 * @param reg   the registered pattern that was matched
 * @param match the match result containing the bound parameter values
 */
public record RegisteredPatternMatch(@NotNull RegisteredPattern reg, @NotNull Match match) {
}
