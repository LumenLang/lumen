package dev.lumenlang.lumen.pipeline.language.simulator.walk;

import dev.lumenlang.lumen.api.util.FuzzyMatch;
import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.SimulatorDebug;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.Trace;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.Verbosity;
import dev.lumenlang.lumen.pipeline.language.simulator.prefilter.result.LiteralInfo;
import dev.lumenlang.lumen.pipeline.language.simulator.score.Fuzzy;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.language.tokenization.TokenKind;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Single-token typo correction. Walks the input tokens, finds the best literal-form swap within
 * the fuzzy threshold, and applies it to produce a corrected token list. Used by the level-0
 * retry and the partial-typo fallback inside {@link TryMatch}.
 */
public final class Typo {

    private Typo() {
    }

    /**
     * Lowest-distance typo fix across {@code tokens}, or {@code null} when no candidate is
     * within the per-pair threshold.
     */
    public static @Nullable Fix findBest(@NotNull List<Token> tokens, @NotNull List<LiteralInfo> literals, @Nullable Pattern pattern, @NotNull SimulatorDebug debug) {
        Fix best = null;
        int bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            for (LiteralInfo lit : literals) {
                for (String form : lit.forms()) {
                    boolean shortLoose = canUseShortLoose(token, lit, tokens, literals);
                    int dist = Fuzzy.distance(token.text(), form, shortLoose);
                    int threshold = Fuzzy.threshold(token.text(), form);
                    boolean within = dist > 0 && dist <= threshold;
                    boolean kept = within && dist < bestDist;
                    if (debug.enabled(Verbosity.DEEP) && pattern != null) {
                        Trace.typoCandidate(debug, pattern, i, token.text(), form, dist, threshold, kept);
                    }
                    if (kept) {
                        bestDist = dist;
                        best = new Fix(token, form, i);
                    }
                }
            }
        }
        return best;
    }

    /**
     * Returns a copy of {@code tokens} with the {@link Fix#token()} replaced by a synthetic
     * token carrying {@link Fix#expected()} as its text.
     */
    public static @NotNull List<Token> apply(@NotNull List<Token> tokens, @NotNull Fix fix) {
        List<Token> result = new ArrayList<>(tokens);
        int line = tokens.isEmpty() ? 1 : tokens.get(0).line();
        result.set(fix.tokenIndex, new Token(TokenKind.IDENT, fix.expected, line, 0, fix.expected.length()));
        return result;
    }

    /**
     * {@code true} when the input's first non-symbol token matches the pattern's first required
     * literal exactly or within one prefix-aware edit.
     */
    public static boolean firstTokenMatches(@NotNull List<Token> tokens, @NotNull List<LiteralInfo> literals) {
        LiteralInfo firstRequired = firstRequiredLiteral(literals);
        if (firstRequired == null) return false;
        Token firstInput = null;
        for (Token t : tokens) {
            if (t.kind() != TokenKind.SYMBOL) {
                firstInput = t;
                break;
            }
        }
        if (firstInput == null) return false;
        for (String form : firstRequired.forms()) {
            if (form.equalsIgnoreCase(firstInput.text())) return true;
            if (FuzzyMatch.prefixAwareDistance(firstInput.text(), form) <= 1) return true;
        }
        return false;
    }

    /**
     * {@code true} when {@code fix} corrects the input's first non-symbol token to the pattern's
     * first required literal.
     */
    public static boolean isFirstLiteralToken(@NotNull Fix fix, @NotNull List<Token> tokens, @NotNull List<LiteralInfo> literals) {
        LiteralInfo firstRequired = firstRequiredLiteral(literals);
        if (firstRequired == null) return false;
        int firstInputIdx = -1;
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).kind() != TokenKind.SYMBOL) {
                firstInputIdx = i;
                break;
            }
        }
        if (firstInputIdx < 0) return false;
        return fix.tokenIndex == firstInputIdx && firstRequired.forms().stream().anyMatch(f -> f.equalsIgnoreCase(fix.expected));
    }

    /**
     * The 1-char-loose typo path is enabled only when the candidate token is 1 character, the
     * target form is 2 characters, and every other required literal in the pattern has at least
     * one exact-match token elsewhere in the input.
     */
    public static boolean canUseShortLoose(@NotNull Token candidate, @NotNull LiteralInfo target, @NotNull List<Token> tokens, @NotNull List<LiteralInfo> literals) {
        if (candidate.text().length() != 1) return false;
        if (target.forms().stream().noneMatch(f -> f.length() == 2)) return false;
        for (LiteralInfo other : literals) {
            if (other == target || other.optional()) continue;
            boolean satisfied = false;
            for (Token t : tokens) {
                for (String form : other.forms()) {
                    if (form.equalsIgnoreCase(t.text())) {
                        satisfied = true;
                        break;
                    }
                }
                if (satisfied) break;
            }
            if (!satisfied) return false;
        }
        return true;
    }

    private static @Nullable LiteralInfo firstRequiredLiteral(@NotNull List<LiteralInfo> literals) {
        for (LiteralInfo lit : literals) {
            if (!lit.optional()) return lit;
        }
        return null;
    }

    /**
     * A typo correction: replace {@code token} at {@code tokenIndex} with {@code expected}.
     *
     * @param token      the original input token
     * @param expected   the literal form proposed as the correction
     * @param tokenIndex index of {@code token} in the input list
     */
    public record Fix(@NotNull Token token, @NotNull String expected, int tokenIndex) {
    }
}
