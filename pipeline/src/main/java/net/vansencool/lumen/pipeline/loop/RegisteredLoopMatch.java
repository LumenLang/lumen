package net.vansencool.lumen.pipeline.loop;

import net.vansencool.lumen.pipeline.language.match.Match;
import org.jetbrains.annotations.NotNull;

/**
 * The result of a successful loop source pattern match, bundling the matched
 * {@link RegisteredLoop} with its corresponding {@link Match}.
 *
 * @param reg   the registered loop source that was matched
 * @param match the match result containing the bound parameter values
 */
public record RegisteredLoopMatch(@NotNull RegisteredLoop reg, @NotNull Match match) {
}
