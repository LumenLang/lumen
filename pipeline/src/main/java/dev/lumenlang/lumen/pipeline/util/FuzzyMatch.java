package dev.lumenlang.lumen.pipeline.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Provides fuzzy string matching using Levenshtein edit distance.
 */
public final class FuzzyMatch {

    private FuzzyMatch() {
    }

    /**
     * Computes the Levenshtein edit distance between two strings (case-insensitive).
     *
     * @param a first string
     * @param b second string
     * @return the minimum number of single-character edits to transform {@code a} into {@code b}
     */
    public static int distance(@NotNull String a, @NotNull String b) {
        String la = a.toLowerCase();
        String lb = b.toLowerCase();
        int[] prev = new int[lb.length() + 1];
        int[] curr = new int[lb.length() + 1];
        for (int j = 0; j <= lb.length(); j++) prev[j] = j;
        for (int i = 1; i <= la.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= lb.length(); j++) {
                int cost = la.charAt(i - 1) == lb.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[lb.length()];
    }

    /**
     * Finds the closest match from a collection of candidates.
     *
     * <p>Returns the candidate with the smallest edit distance, provided it falls within
     * a reasonable threshold (at most 40% of the input length, minimum 1, maximum 3).
     *
     * @param input      the misspelled input
     * @param candidates the collection of valid names to match against
     * @return the closest match, or {@code null} if nothing is close enough
     */
    public static @Nullable String closest(@NotNull String input, @NotNull Collection<String> candidates) {
        int threshold = Math.max(1, Math.min(3, (int) (input.length() * 0.4)));
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        for (String candidate : candidates) {
            int dist = distance(input, candidate);
            if (dist < bestDist && dist <= threshold) {
                bestDist = dist;
                best = candidate;
            }
        }
        return best;
    }
}
