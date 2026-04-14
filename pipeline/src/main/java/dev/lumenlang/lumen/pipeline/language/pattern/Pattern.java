package dev.lumenlang.lumen.pipeline.language.pattern;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A compiled representation of a Lumen pattern string.
 *
 * <p>Patterns are matched against tokenized input to identify statement and expression
 * forms. Each pattern is pre-analyzed to determine token count bounds for fast
 * candidate filtering during pattern index lookups.
 *
 * @param raw       the original, unmodified pattern string
 * @param parts     the ordered list of literal and placeholder parts
 * @param minTokens the minimum number of tokens required to match this pattern (for early rejection)
 * @param maxTokens the maximum number of tokens this pattern can consume (or Integer.MAX_VALUE if greedy)
 */
public record Pattern(@NotNull String raw, @NotNull List<PatternPart> parts, int minTokens, int maxTokens) {

    public Pattern(@NotNull String raw, @NotNull List<PatternPart> parts) {
        this(raw, parts, computeMinTokens(parts), computeMaxTokens(parts));
    }

    private static int computeMinTokens(@NotNull List<PatternPart> parts) {
        int count = 0;
        for (PatternPart part : parts) {
            if (part instanceof PatternPart.Literal) count++;
            else if (part instanceof PatternPart.FlexLiteral) count++;
            else if (part instanceof PatternPart.PlaceholderPart) count++;
            else if (part instanceof PatternPart.Group group) {
                if (group.required()) {
                    int min = Integer.MAX_VALUE;
                    for (List<PatternPart> alt : group.alternatives()) {
                        min = Math.min(min, computeMinTokens(alt));
                    }
                    count += min;
                }
            }
        }
        return count;
    }

    private static int computeMaxTokens(@NotNull List<PatternPart> parts) {
        int count = 0;
        boolean hasGreedy = false;
        for (PatternPart part : parts) {
            if (part instanceof PatternPart.Literal) count++;
            else if (part instanceof PatternPart.FlexLiteral) count++;
            else if (part instanceof PatternPart.PlaceholderPart) hasGreedy = true;
            else if (part instanceof PatternPart.Group group) {
                int max = 0;
                for (List<PatternPart> alt : group.alternatives()) {
                    int altMax = computeMaxTokens(alt);
                    if (altMax == Integer.MAX_VALUE) return Integer.MAX_VALUE;
                    max = Math.max(max, altMax);
                }
                if (max == Integer.MAX_VALUE) return Integer.MAX_VALUE;
                count += max;
            }
        }
        return hasGreedy ? Integer.MAX_VALUE : count;
    }
}