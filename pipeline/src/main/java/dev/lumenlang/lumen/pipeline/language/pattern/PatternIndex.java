package dev.lumenlang.lumen.pipeline.language.pattern;

import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Literal based index for fast pattern candidate filtering.
 *
 * <p>Extracts all required literal tokens from each pattern. When matching input
 * tokens, only patterns whose required literals are ALL present in the input are
 * returned as candidates. Patterns with no required literals (all placeholders)
 * are always included.
 *
 * <p>Uses a first-token dispatch map for fast initial narrowing before checking
 * additional required literals.
 *
 * <p>The original priority ordering of the input list is preserved, so specificity
 * based matching continues to work correctly.
 *
 * @param <T> the registered pattern wrapper type
 */
public final class PatternIndex<T> {

    private final List<T> all;
    private final List<Set<String>> requiredLiterals;
    private final int[] minTokenCounts;
    private final int[] maxTokenCounts;
    private final Map<String, List<Integer>> firstTokenMap;
    private final List<Integer> wildcardIndices;

    /**
     * Builds an index from a pre-sorted list of pattern items.
     *
     * @param sortedItems      the patterns in priority order (highest priority first)
     * @param patternExtractor function to extract the compiled {@link Pattern} from each item
     */
    public PatternIndex(@NotNull List<T> sortedItems, @NotNull Function<T, Pattern> patternExtractor) {
        this.all = List.copyOf(sortedItems);
        this.requiredLiterals = new ArrayList<>(sortedItems.size());
        this.minTokenCounts = new int[sortedItems.size()];
        this.maxTokenCounts = new int[sortedItems.size()];
        this.firstTokenMap = new HashMap<>();
        this.wildcardIndices = new ArrayList<>();

        for (int i = 0; i < sortedItems.size(); i++) {
            Pattern pattern = patternExtractor.apply(sortedItems.get(i));
            Set<String> literals = new HashSet<>();
            collectRequiredLiterals(pattern.parts(), literals);
            requiredLiterals.add(literals);
            minTokenCounts[i] = pattern.minTokens();
            maxTokenCounts[i] = pattern.maxTokens();

            String firstLiteral = firstLiteral(pattern.parts());
            if (firstLiteral != null) {
                firstTokenMap.computeIfAbsent(firstLiteral, k -> new ArrayList<>()).add(i);
            } else {
                wildcardIndices.add(i);
            }
        }
    }

    /**
     * Returns the candidate patterns that could possibly match the given input tokens.
     *
     * @param tokens the input tokens to match against
     * @return a narrowed list of candidates in priority order
     */
    public @NotNull List<T> candidates(@NotNull List<Token> tokens) {
        if (tokens.isEmpty()) return all;

        int tokenCount = tokens.size();
        String firstInput = tokens.get(0).text().toLowerCase(Locale.ROOT);
        List<Integer> firstMatches = firstTokenMap.get(firstInput);

        if (firstMatches == null && wildcardIndices.isEmpty()) return List.of();

        Set<String> inputTokens = null;

        List<T> result = new ArrayList<>();
        if (firstMatches != null) {
            for (int idx : firstMatches) {
                if (minTokenCounts[idx] > tokenCount || maxTokenCounts[idx] < tokenCount) continue;
                Set<String> needed = requiredLiterals.get(idx);
                if (needed.size() <= 1) {
                    result.add(all.get(idx));
                } else {
                    if (inputTokens == null) inputTokens = buildTokenSet(tokens);
                    if (inputTokens.containsAll(needed)) result.add(all.get(idx));
                }
            }
        }

        for (int idx : wildcardIndices) {
            if (minTokenCounts[idx] > tokenCount || maxTokenCounts[idx] < tokenCount) continue;
            Set<String> needed = requiredLiterals.get(idx);
            if (needed.isEmpty()) {
                result.add(all.get(idx));
            } else {
                if (inputTokens == null) inputTokens = buildTokenSet(tokens);
                if (inputTokens.containsAll(needed)) result.add(all.get(idx));
            }
        }

        return result;
    }

    /**
     * Returns all indexed patterns in their original priority order.
     *
     * @return the full pattern list
     */
    public @NotNull List<T> all() {
        return all;
    }

    private static @NotNull Set<String> buildTokenSet(@NotNull List<Token> tokens) {
        Set<String> set = new HashSet<>(tokens.size());
        for (Token t : tokens) {
            set.add(t.text().toLowerCase(Locale.ROOT));
        }
        return set;
    }

    private static String firstLiteral(@NotNull List<PatternPart> parts) {
        if (parts.isEmpty()) return null;
        PatternPart first = parts.get(0);
        if (first instanceof PatternPart.Literal lit) return lit.text();
        if (first instanceof PatternPart.FlexLiteral flex && flex.forms().size() == 1) return flex.forms().iterator().next();
        return null;
    }

    /**
     * Collects all literals that MUST appear in the input for this pattern to match.
     * Only includes literals from required positions (not inside optional groups).
     * For required choice groups, only literals present in ALL alternatives are included.
     */
    private static void collectRequiredLiterals(@NotNull List<PatternPart> parts, @NotNull Set<String> out) {
        for (PatternPart part : parts) {
            if (part instanceof PatternPart.Literal lit) {
                out.add(lit.text());
            } else if (part instanceof PatternPart.FlexLiteral flex) {
                Set<String> common = new HashSet<>(flex.forms());
                if (common.size() == 1) out.addAll(common);
            } else if (part instanceof PatternPart.Group group) {
                if (!group.required()) continue;
                Set<String> common = null;
                for (List<PatternPart> alt : group.alternatives()) {
                    Set<String> altLiterals = new HashSet<>();
                    collectRequiredLiterals(alt, altLiterals);
                    if (common == null) {
                        common = altLiterals;
                    } else {
                        common.retainAll(altLiterals);
                    }
                }
                if (common != null) out.addAll(common);
            }
        }
    }
}
