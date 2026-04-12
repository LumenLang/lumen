package dev.lumenlang.lumen.api.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Fuzzy string matching using Levenshtein and Damerau-Levenshtein edit distance,
 * with prefix aware scoring for pattern literal matching.
 */
public final class FuzzyMatch {

    private FuzzyMatch() {
    }

    /**
     * Computes the Levenshtein edit distance between two strings (case insensitive).
     *
     * @param a first string
     * @param b second string
     * @return the minimum number of single character edits to transform {@code a} into {@code b}
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
     * Computes the Damerau-Levenshtein distance between two strings (case insensitive).
     * Unlike plain Levenshtein, this treats adjacent character transpositions as a single edit.
     *
     * @param a first string
     * @param b second string
     * @return the minimum number of edits (insert, delete, substitute, transpose) to transform {@code a} into {@code b}
     */
    public static int damerauLevenshteinDistance(@NotNull String a, @NotNull String b) {
        String la = a.toLowerCase();
        String lb = b.toLowerCase();
        int lenA = la.length();
        int lenB = lb.length();
        int[][] d = new int[lenA + 1][lenB + 1];
        for (int i = 0; i <= lenA; i++) d[i][0] = i;
        for (int j = 0; j <= lenB; j++) d[0][j] = j;
        for (int i = 1; i <= lenA; i++) {
            for (int j = 1; j <= lenB; j++) {
                int cost = la.charAt(i - 1) == lb.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
                if (i > 1 && j > 1 && la.charAt(i - 1) == lb.charAt(j - 2) && la.charAt(i - 2) == lb.charAt(j - 1)) {
                    d[i][j] = Math.min(d[i][j], d[i - 2][j - 2] + 1);
                }
            }
        }
        return d[lenA][lenB];
    }

    /**
     * Computes a prefix aware fuzzy distance between an input token and a pattern literal.
     * First tries standard Levenshtein. If that exceeds the threshold, checks whether the input
     * is a truncated or transposed prefix of the literal using Damerau-Levenshtein against
     * progressively longer prefixes.
     *
     * @param input   the input token text
     * @param literal the pattern literal text
     * @return the effective distance, potentially lower than plain Levenshtein for prefix matches
     */
    public static int prefixAwareDistance(@NotNull String input, @NotNull String literal) {
        int stdThreshold = input.length() <= 2 ? 0 : Math.max(1, Math.min(3, (int) (input.length() * 0.4)));
        int directDist = distance(input, literal);
        if (directDist <= stdThreshold) return directDist;
        int dlFull = damerauLevenshteinDistance(input, literal);
        if (dlFull <= stdThreshold) return dlFull;
        int minLen = Math.max(2, input.length() - 2);
        int maxLen = Math.min(literal.length(), input.length() + 2);
        if (minLen > maxLen || minLen > literal.length()) return Math.min(directDist, dlFull);
        int bestPrefixDist = Integer.MAX_VALUE;
        for (int len = minLen; len <= maxLen; len++) {
            int dl = damerauLevenshteinDistance(input, literal.substring(0, len));
            if (dl < bestPrefixDist) bestPrefixDist = dl;
        }
        if (bestPrefixDist <= 1) return bestPrefixDist + 1;
        return Math.min(directDist, dlFull);
    }

    /**
     * Finds the closest match from a collection of candidates using prefix aware fuzzy matching.
     *
     * <p>Returns the candidate with the smallest effective distance, provided it falls within
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
            int dist = prefixAwareDistance(input, candidate);
            if (dist < bestDist && dist <= threshold) {
                bestDist = dist;
                best = candidate;
            }
        }
        return best;
    }
}
