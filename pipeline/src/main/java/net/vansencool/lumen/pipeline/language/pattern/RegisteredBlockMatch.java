package net.vansencool.lumen.pipeline.language.pattern;

import net.vansencool.lumen.pipeline.codegen.TypeEnv;
import net.vansencool.lumen.pipeline.language.match.Match;
import org.jetbrains.annotations.NotNull;

/**
 * The result of a successful block pattern match, bundling the matched
 * {@link RegisteredBlock} with its corresponding {@link Match}.
 *
 * <p>Returned by {@link PatternRegistry#matchBlock(java.util.List, TypeEnv)}
 * when a block header's token list matches a registered block pattern.
 *
 * @param reg   the registered block that was matched
 * @param match the match result containing the bound parameter values
 */
public record RegisteredBlockMatch(@NotNull RegisteredBlock reg, @NotNull Match match) {
}
