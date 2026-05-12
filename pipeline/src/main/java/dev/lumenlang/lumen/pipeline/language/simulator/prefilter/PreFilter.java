package dev.lumenlang.lumen.pipeline.language.simulator.prefilter;

import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.ScoreBreakdown;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.SimulatorDebug;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.Trace;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.Verbosity;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.trace.TraceEvent;
import dev.lumenlang.lumen.pipeline.language.simulator.options.SimulatorOption;
import dev.lumenlang.lumen.pipeline.language.simulator.options.SimulatorOptions;
import dev.lumenlang.lumen.pipeline.language.simulator.prefilter.result.LiteralInfo;
import dev.lumenlang.lumen.pipeline.language.simulator.prefilter.result.LiteralMatchResult;
import dev.lumenlang.lumen.pipeline.language.simulator.prefilter.result.PreFilterScore;
import dev.lumenlang.lumen.pipeline.language.simulator.score.Fuzzy;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.language.tokenization.TokenKind;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Scores registered patterns against the input tokens using fuzzy literal matching, weighted by
 * coverage, exactness, ordering, and first-token alignment. Patterns below
 * {@code MIN_PREFILTER_CONFIDENCE} are dropped before the expensive analysis pass.
 */
public final class PreFilter {

    private PreFilter() {
    }

    /**
     * Scores {@code pattern} against {@code tokens} and returns a {@link PreFilterScore} when the
     * combined confidence clears {@code MIN_PREFILTER_CONFIDENCE}. {@code null} otherwise.
     */
    public static @Nullable PreFilterScore run(@NotNull List<Token> tokens, @NotNull Pattern pattern, @Nullable Object handler, @NotNull SimulatorOptions opts, @NotNull SimulatorDebug debug) {
        List<LiteralInfo> literals = LiteralInfo.extract(pattern);
        if (literals.isEmpty()) {
            Trace.preFilterReject(debug, pattern, "no literals to anchor");
            return null;
        }
        boolean[] tokenUsed = new boolean[tokens.size()];
        List<LiteralMatchResult> matches = new ArrayList<>();
        for (LiteralInfo lit : literals) {
            int bestIdx = -1;
            int bestDist = Integer.MAX_VALUE;
            for (int j = 0; j < tokens.size(); j++) {
                if (tokenUsed[j]) continue;
                int dist = bestFormDistance(tokens.get(j).text(), lit);
                if (debug.enabled(Verbosity.DEEP)) {
                    int threshold = Fuzzy.threshold(tokens.get(j).text(), lit.primaryForm());
                    Trace.literalProbe(debug, pattern, j, tokens.get(j).text(), lit.primaryForm(), dist, threshold, dist <= threshold);
                }
                if (dist < bestDist) {
                    bestDist = dist;
                    bestIdx = j;
                }
            }
            int threshold = bestIdx >= 0 ? Fuzzy.threshold(tokens.get(bestIdx).text(), lit.primaryForm()) : 0;
            if (bestIdx >= 0 && bestDist <= threshold) {
                tokenUsed[bestIdx] = true;
                matches.add(new LiteralMatchResult(lit, bestIdx, bestDist));
            } else if (!lit.optional()) {
                matches.add(new LiteralMatchResult(lit, -1, bestDist));
            }
        }
        int requiredCount = (int) literals.stream().filter(l -> !l.optional()).count();
        if (requiredCount == 0) {
            Trace.preFilterReject(debug, pattern, "no required literals");
            return null;
        }
        int matchedRequired = (int) matches.stream().filter(m -> m.tokenIndex() >= 0 && !m.literal().optional()).count();
        if (matchedRequired == 0) {
            Trace.preFilterReject(debug, pattern, "no required literal matched (0/" + requiredCount + ")");
            return null;
        }
        int matchedTotal = (int) matches.stream().filter(m -> m.tokenIndex() >= 0).count();
        int exactMatches = (int) matches.stream().filter(m -> m.tokenIndex() >= 0 && m.distance() == 0).count();
        double literalCoverage = matchedRequired / (double) requiredCount;
        double tokenCoverage = tokens.isEmpty() ? 0.0 : matchedTotal / (double) tokens.size();
        double exactness = matchedTotal > 0 ? exactMatches / (double) matchedTotal : 0.0;
        List<Integer> positions = matches.stream().map(LiteralMatchResult::tokenIndex).filter(idx -> idx >= 0).toList();
        double positionAccuracy;
        if (positions.size() <= 1) {
            positionAccuracy = 0.5;
        } else {
            int ordered = 0;
            for (int p = 1; p < positions.size(); p++) {
                if (positions.get(p) > positions.get(p - 1)) ordered++;
            }
            positionAccuracy = ordered / (double) (positions.size() - 1);
        }
        double base = literalCoverage * opts.doubleValue(SimulatorOption.WEIGHT_LITERAL_COVERAGE) + exactness * opts.doubleValue(SimulatorOption.WEIGHT_EXACTNESS) + positionAccuracy * opts.doubleValue(SimulatorOption.WEIGHT_POSITION) + tokenCoverage * opts.doubleValue(SimulatorOption.WEIGHT_TOKEN_COVERAGE);
        double firstMultiplier = firstTokenMultiplier(tokens, literals, matches, opts);
        double confidence = Math.min(1.0, base * firstMultiplier);
        double minConf = opts.doubleValue(SimulatorOption.MIN_PREFILTER_CONFIDENCE);
        boolean admitted = confidence >= minConf;
        ScoreBreakdown breakdown = new ScoreBreakdown(literalCoverage, exactness, positionAccuracy, tokenCoverage, firstMultiplier, base, confidence);
        debug.trace(new TraceEvent.CandidateScored(pattern, admitted, breakdown));
        debug.emit(Verbosity.BREAKDOWN, 1, () -> (admitted ? "+ " : "- ") + pattern.raw() + "  " + breakdown.oneLine());
        if (!admitted) {
            Trace.preFilterReject(debug, pattern, "confidence " + String.format("%.3f", confidence) + " < MIN_PREFILTER " + String.format("%.3f", minConf));
            return null;
        }
        return new PreFilterScore(pattern, confidence, matches, handler);
    }

    private static int bestFormDistance(@NotNull String tokenText, @NotNull LiteralInfo lit) {
        int best = Integer.MAX_VALUE;
        for (String form : lit.forms()) {
            int dist = Fuzzy.distance(tokenText, form);
            if (dist < best) best = dist;
        }
        return best;
    }

    private static double firstTokenMultiplier(@NotNull List<Token> tokens, @NotNull List<LiteralInfo> literals, @NotNull List<LiteralMatchResult> matches, @NotNull SimulatorOptions opts) {
        double miss = opts.doubleValue(SimulatorOption.FIRST_TOKEN_MISS_MULTIPLIER);
        LiteralInfo firstRequired = null;
        for (LiteralInfo lit : literals) {
            if (!lit.optional()) {
                firstRequired = lit;
                break;
            }
        }
        if (firstRequired == null) return miss;
        int firstInputIdx = -1;
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).kind() != TokenKind.SYMBOL) {
                firstInputIdx = i;
                break;
            }
        }
        if (firstInputIdx < 0) return miss;
        for (LiteralMatchResult m : matches) {
            if (m.literal() == firstRequired && m.tokenIndex() == firstInputIdx) {
                return m.distance() == 0 ? 1.0 : 0.85;
            }
        }
        return miss;
    }
}
