package dev.lumenlang.lumen.pipeline.language.simulator.score;

import dev.lumenlang.lumen.api.util.FuzzyMatch;
import org.jetbrains.annotations.NotNull;

/**
 * Fuzzy-distance primitives used by the pre-filter and typo paths. Delegates to
 * {@link FuzzyMatch} but applies length-aware thresholds and selects between the
 * prefix-aware and plain Damerau-Levenshtein scorers based on token length.
 */
public final class Fuzzy {

    private Fuzzy() {
    }

    /**
     * Distance between {@code tokenText} and {@code formText}. 2-char tokens use plain
     * Damerau-Levenshtein because the prefix-penalty scheme inflates the distance for genuine
     * single-edit typos like {@code st} for {@code set}.
     */
    public static int distance(@NotNull String tokenText, @NotNull String formText) {
        return distance(tokenText, formText, false);
    }

    /**
     * {@code shortTokenLooseDistance=true} drops the prefix-aware penalty for 1-char tokens, so a
     * lone {@code t} can typo-fix to {@code to}. Callers enable it only when the pattern's other
     * required literals are already accounted for, otherwise random 1-char tokens would match
     * arbitrary 2-char keywords.
     */
    public static int distance(@NotNull String tokenText, @NotNull String formText, boolean shortTokenLooseDistance) {
        if (shortTokenLooseDistance && tokenText.length() == 1 && formText.length() == 2) {
            return FuzzyMatch.damerauLevenshteinDistance(tokenText, formText);
        }
        if (tokenText.length() == 2 && formText.length() >= 2 && formText.length() <= 3) {
            return FuzzyMatch.damerauLevenshteinDistance(tokenText, formText);
        }
        return FuzzyMatch.prefixAwareDistance(tokenText, formText);
    }

    /**
     * Maximum distance under which {@code tokenText} is still considered a typo of {@code formText}.
     * Scales with token and form length, with bumps for short tokens that share a leading char
     * and a relaxed bonus when the token is a permutation of the form.
     */
    public static int threshold(@NotNull String tokenText, @NotNull String formText) {
        int tokenLen = tokenText.length();
        int formLen = formText.length();
        if (tokenLen == 1 && formLen == 2 && tokenText.charAt(0) == formText.charAt(0)) {
            return 1;
        }
        if (tokenLen == 2 && formLen >= 2 && formLen <= tokenLen + 1) {
            return 1;
        }
        int tokenThreshold = tokenLen <= 2 ? 0 : Math.max(1, Math.min(3, (int) (tokenLen * 0.4)));
        int formThreshold = formLen <= 2 ? 0 : Math.max(1, Math.min(3, (int) (formLen * 0.4)));
        int base = Math.min(tokenThreshold, formThreshold);
        if (base > 0 && tokenLen >= 5 && isCharBagSubset(tokenText, formText)) {
            return Math.min(3, base + 1);
        }
        return base;
    }

    /**
     * {@code true} when every character in {@code token} (case folded, counting multiplicity)
     * appears in {@code form}.
     */
    public static boolean isCharBagSubset(@NotNull String token, @NotNull String form) {
        String t = token.toLowerCase();
        String f = form.toLowerCase();
        int[] counts = new int[128];
        for (int i = 0; i < f.length(); i++) {
            char c = f.charAt(i);
            if (c < 128) counts[c]++;
        }
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c >= 128 || counts[c] == 0) return false;
            counts[c]--;
        }
        return true;
    }
}
