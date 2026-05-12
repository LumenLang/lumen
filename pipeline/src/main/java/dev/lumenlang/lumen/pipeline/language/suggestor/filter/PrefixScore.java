package dev.lumenlang.lumen.pipeline.language.suggestor.filter;

import org.jetbrains.annotations.NotNull;

/**
 * Strength of a prefix match between a partial token and a target string.
 */
public final class PrefixScore {

    private PrefixScore() {
    }

    /**
     * {@code 1 - remaining/totalLength}: 1.0 when prefix equals target, near 0 when only one
     * char of a long target was typed. Empty prefix scores 0 (no signal). Returns 0 when
     * target does not start with prefix.
     */
    public static double of(@NotNull String prefix, @NotNull String target) {
        if (target.isEmpty()) return 0.0;
        if (prefix.isEmpty()) return 0.0;
        if (!PrefixFilter.matches(target, prefix)) return 0.0;
        double matched = prefix.length();
        double total = target.length();
        return Math.min(1.0, matched / total);
    }
}
