package dev.lumenlang.lumen.pipeline.language.pattern;

import dev.lumenlang.lumen.pipeline.language.match.Match;
import org.jetbrains.annotations.NotNull;

/**
 * Captures a successful expression pattern match, pairing the {@link RegisteredExpression}
 * (pattern + handler) with the bound parameter values from the match.
 *
 * @param reg   the registered expression pattern and handler
 * @param match the match result containing bound parameters
 */
public record RegisteredExpressionMatch(@NotNull RegisteredExpression reg, @NotNull Match match) {
}
