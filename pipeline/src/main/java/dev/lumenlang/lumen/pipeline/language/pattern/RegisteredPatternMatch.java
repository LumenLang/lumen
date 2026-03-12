package dev.lumenlang.lumen.pipeline.language.pattern;

import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.language.match.Match;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The result of a successful statement pattern match, bundling the matched
 * {@link RegisteredPattern} with its corresponding {@link Match}.
 *
 * <p>Returned by {@link PatternRegistry#matchStatement(List, TypeEnv)}
 * when a statement's token list matches a registered pattern.
 *
 * @param reg   the registered pattern that was matched
 * @param match the match result containing the bound parameter values
 */
public record RegisteredPatternMatch(@NotNull RegisteredPattern reg, @NotNull Match match) {
}
