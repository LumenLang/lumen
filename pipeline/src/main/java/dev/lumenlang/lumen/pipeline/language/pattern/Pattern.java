package dev.lumenlang.lumen.pipeline.language.pattern;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A compiled representation of a Lumen pattern string.
 *
 * <p>Patterns are matched against tokenized input to identify statement and expression
 * forms. Each pattern is pre-analyzed to determine the minimum token count for fast
 * candidate filtering during pattern index lookups.
 *
 * @param raw       the original, unmodified pattern string
 * @param parts     the ordered list of literal and placeholder parts
 * @param minTokens the minimum number of tokens required to match this pattern (for early rejection)
 */
public record Pattern(@NotNull String raw, @NotNull List<PatternPart> parts, int minTokens) {

    public Pattern(@NotNull String raw, @NotNull List<PatternPart> parts) {
        this(raw, parts, computeMinTokens(parts));
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
}