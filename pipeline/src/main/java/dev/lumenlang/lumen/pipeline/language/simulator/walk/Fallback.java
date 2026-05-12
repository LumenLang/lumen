package dev.lumenlang.lumen.pipeline.language.simulator.walk;

import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternPart;
import dev.lumenlang.lumen.pipeline.language.simulator.prefilter.result.LiteralInfo;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Pattern-shape helpers for the type-mismatch and partial-typo fallback paths.
 */
public final class Fallback {

    private Fallback() {
    }

    /**
     * Computes the column where a caret should land for a missing binding. Walks the pattern
     * parts to locate {@code bindingId} and the literal {@code failed} matches; if the literal
     * comes before the placeholder, the gap is right after the literal, otherwise it is right
     * before. When {@code failed} is null the gap is past the end of input.
     */
    public static int missingBindingColumn(@NotNull Pattern pattern, @NotNull String bindingId, @Nullable Token failed, @NotNull List<Token> tokens) {
        if (failed == null) {
            if (tokens.isEmpty()) return 0;
            return tokens.get(tokens.size() - 1).end() + 1;
        }
        int placeholderIdx = -1;
        int literalIdx = -1;
        List<PatternPart> parts = pattern.parts();
        for (int i = 0; i < parts.size(); i++) {
            PatternPart part = parts.get(i);
            if (placeholderIdx < 0 && part instanceof PatternPart.PlaceholderPart pp && pp.ph().typeId().equals(bindingId)) {
                placeholderIdx = i;
            }
            if (literalIdx < 0 && partMatchesText(part, failed.text())) {
                literalIdx = i;
            }
        }
        if (literalIdx >= 0 && literalIdx < placeholderIdx) {
            return failed.end() + 1;
        }
        return Math.max(0, failed.start() - 1);
    }

    /**
     * Returns {@code true} when {@code failed} matches any literal form in the pattern. The
     * matcher fed this token to a placeholder, but the token actually belongs to a later literal
     * slot, so the placeholder should be reported as missing rather than as a type mismatch.
     */
    public static boolean tokenIsLaterPatternLiteral(@NotNull Token failed, @NotNull List<LiteralInfo> literals) {
        String text = failed.text();
        for (LiteralInfo lit : literals) {
            for (String form : lit.forms()) {
                if (form.equalsIgnoreCase(text)) return true;
            }
        }
        return false;
    }

    private static boolean partMatchesText(@NotNull PatternPart part, @NotNull String text) {
        if (part instanceof PatternPart.Literal lit) return lit.text().equalsIgnoreCase(text);
        if (part instanceof PatternPart.FlexLiteral flex) {
            for (String form : flex.forms()) {
                if (form.equalsIgnoreCase(text)) return true;
            }
        }
        if (part instanceof PatternPart.Group group) {
            for (List<PatternPart> alt : group.alternatives()) {
                for (PatternPart inner : alt) {
                    if (partMatchesText(inner, text)) return true;
                }
            }
        }
        return false;
    }
}
