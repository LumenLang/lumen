package dev.lumenlang.lumen.pipeline.language.pattern;

import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
 * <p>The original priority ordering of the input list is preserved, so specificity
 * based matching continues to work correctly.
 *
 * @param <T> the registered pattern wrapper type
 */
public final class PatternIndex<T> {

    private final List<T> all;
    private final List<Set<String>> requiredLiterals;

    /**
     * Builds an index from a pre-sorted list of pattern items.
     *
     * @param sortedItems      the patterns in priority order (highest priority first)
     * @param patternExtractor function to extract the compiled {@link Pattern} from each item
     */
    public PatternIndex(@NotNull List<T> sortedItems, @NotNull Function<T, Pattern> patternExtractor) {
        this.all = List.copyOf(sortedItems);
        this.requiredLiterals = new ArrayList<>(sortedItems.size());
        for (T item : sortedItems) {
            Set<String> literals = new HashSet<>();
            collectRequiredLiterals(patternExtractor.apply(item).parts(), literals);
            requiredLiterals.add(literals);
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
        Set<String> inputTokens = new HashSet<>(tokens.size());
        for (Token t : tokens) {
            inputTokens.add(t.text().toLowerCase(Locale.ROOT));
        }
        List<T> result = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            if (inputTokens.containsAll(requiredLiterals.get(i))) {
                result.add(all.get(i));
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
