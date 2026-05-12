package dev.lumenlang.lumen.pipeline.language.suggestor.rank;

import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternPart;
import dev.lumenlang.lumen.pipeline.language.simulator.result.Position;
import dev.lumenlang.lumen.pipeline.language.suggestor.token.ActiveToken;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * Editor-side ranking score for a {@link Position}. Combines pre-filter confidence, walk
 * progress, pattern specificity, and an active-prefix boost when the cursor's prefix matches
 * the literal the walk stopped at.
 */
public final class Ranker {

    private Ranker() {
    }

    /**
     * Composite score in {@code 0..1}.
     */
    public static double score(@NotNull Position position, int inputTokenCount, @NotNull ActiveToken active) {
        double confidence = position.confidence();
        double progress = inputTokenCount == 0 ? 1.0 : (double) position.consumedTokens() / inputTokenCount;
        double specificity = specificity(position.pattern());
        double prefixBoost = prefixBoost(position, active);
        double firstLiteralBoost = firstLiteralBoost(position, inputTokenCount);
        double base = confidence * 0.35 + progress * 0.20 + specificity * 0.15 + prefixBoost * 0.20 + firstLiteralBoost * 0.10;
        return clamp(base);
    }

    /**
     * 1.0 when the input is empty and the pattern's first part is a concrete literal, so the
     * suggestor surfaces typical keyword starts instead of placeholder-leading patterns.
     */
    public static double firstLiteralBoost(@NotNull Position position, int inputTokenCount) {
        if (inputTokenCount > 0) return 0.0;
        List<PatternPart> parts = position.pattern().parts();
        if (parts.isEmpty()) return 0.0;
        PatternPart first = parts.get(0);
        if (first instanceof PatternPart.Literal || first instanceof PatternPart.FlexLiteral) return 1.0;
        if (first instanceof PatternPart.Group group && group.required()) {
            for (List<PatternPart> alt : group.alternatives()) {
                if (alt.isEmpty()) continue;
                PatternPart inner = alt.get(0);
                if (inner instanceof PatternPart.Literal || inner instanceof PatternPart.FlexLiteral) return 1.0;
            }
        }
        return 0.0;
    }

    /**
     * 1.0 when the cursor's prefix matches at least one form of the literal the walk landed on,
     * 0.0 otherwise. Placeholders and group-stops score 0 because their value space is wide.
     */
    public static double prefixBoost(@NotNull Position position, @NotNull ActiveToken active) {
        if (active.text().isEmpty() || position.atPart() == null) return 0.0;
        String lower = active.text().toLowerCase(Locale.ROOT);
        if (position.atPart() instanceof PatternPart.Literal lit) {
            return lit.text().toLowerCase(Locale.ROOT).startsWith(lower) ? 1.0 : 0.0;
        }
        if (position.atPart() instanceof PatternPart.FlexLiteral flex) {
            for (String form : flex.forms()) {
                if (form.toLowerCase(Locale.ROOT).startsWith(lower)) return 1.0;
            }
            return 0.0;
        }
        if (position.atPart() instanceof PatternPart.Group group) {
            for (List<PatternPart> alt : group.alternatives()) {
                if (alt.isEmpty()) continue;
                PatternPart first = alt.get(0);
                if (first instanceof PatternPart.Literal lit && lit.text().toLowerCase(Locale.ROOT).startsWith(lower))
                    return 1.0;
                if (first instanceof PatternPart.FlexLiteral flex) {
                    for (String form : flex.forms()) {
                        if (form.toLowerCase(Locale.ROOT).startsWith(lower)) return 1.0;
                    }
                }
            }
        }
        return 0.0;
    }

    /**
     * Fraction of pattern parts that are concrete literals. Catch-all patterns score low.
     */
    public static double specificity(@NotNull Pattern pattern) {
        List<PatternPart> parts = pattern.parts();
        if (parts.isEmpty()) return 0.0;
        int literals = 0;
        for (PatternPart p : parts) {
            if (p instanceof PatternPart.Literal || p instanceof PatternPart.FlexLiteral) literals++;
        }
        return (double) literals / parts.size();
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }
}
