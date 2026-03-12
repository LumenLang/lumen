package dev.lumenlang.lumen.pipeline.conditions.registry;

import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.conditions.ConditionAtom;
import dev.lumenlang.lumen.pipeline.language.match.Match;

import java.util.List;

/**
 * The result of a successful condition pattern match, pairing the matched
 * {@link RegisteredCondition} with its {@link Match}.
 *
 * <p>Returned by {@link ConditionRegistry#match(List, TypeEnv)}
 * and used by {@link ConditionAtom} to invoke the handler.
 *
 * @param reg   the registered condition that was matched
 * @param match the match result containing the bound parameter values
 */
public record RegisteredConditionMatch(RegisteredCondition reg, Match match) {
}