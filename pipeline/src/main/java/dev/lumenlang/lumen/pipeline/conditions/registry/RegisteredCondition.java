package dev.lumenlang.lumen.pipeline.conditions.registry;

import dev.lumenlang.lumen.api.handler.ConditionHandler;
import dev.lumenlang.lumen.api.pattern.PatternMeta;
import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import org.jetbrains.annotations.NotNull;

/**
 * Associates a compiled {@link Pattern} with the {@link ConditionHandler} that generates a Java
 * boolean expression when the pattern matches a condition fragment.
 *
 * @param pattern the compiled condition pattern
 * @param handler the handler that generates the Java boolean expression
 * @param meta    documentation metadata for this condition
 * @see ConditionRegistry#register(String, ConditionHandler)
 * @see RegisteredConditionMatch
 */
public record RegisteredCondition(@NotNull Pattern pattern, @NotNull ConditionHandler handler,
                                  @NotNull PatternMeta meta) {
}