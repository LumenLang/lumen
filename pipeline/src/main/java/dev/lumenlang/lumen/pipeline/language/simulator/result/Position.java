package dev.lumenlang.lumen.pipeline.language.simulator.result;

import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternPart;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Where a directional pattern walk landed against a given input.
 *
 * @param pattern        the candidate pattern walked
 * @param confidence     pre-filter confidence for the pattern (0..1)
 * @param consumedTokens count of input tokens fully matched before the walk stopped
 * @param atPart         the pattern part the walk stopped at, or {@code null} when every part
 *                       was consumed and the pattern is complete
 * @param atBindingId    type binding id when {@code atPart} is a placeholder, otherwise
 *                       {@code null}
 * @param remainingParts pattern parts after {@code atPart}, or empty when the pattern is complete
 * @param boundSoFar     placeholder name to consumed tokens, in declaration order
 */
public record Position(@NotNull Pattern pattern, double confidence, int consumedTokens,
                       @Nullable PatternPart atPart, @Nullable String atBindingId,
                       @NotNull List<PatternPart> remainingParts,
                       @NotNull Map<String, List<Token>> boundSoFar) {
}
