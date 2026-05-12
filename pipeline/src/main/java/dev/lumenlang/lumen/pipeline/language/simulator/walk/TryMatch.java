package dev.lumenlang.lumen.pipeline.language.simulator.walk;

import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.util.FuzzyMatch;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.language.match.MatchProgress;
import dev.lumenlang.lumen.pipeline.language.match.PatternMatcher;
import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.SimulatorDebug;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.Trace;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.Verbosity;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.trace.TraceEvent;
import dev.lumenlang.lumen.pipeline.language.simulator.options.SimulatorOption;
import dev.lumenlang.lumen.pipeline.language.simulator.options.SimulatorOptions;
import dev.lumenlang.lumen.pipeline.language.simulator.prefilter.result.LiteralInfo;
import dev.lumenlang.lumen.pipeline.language.simulator.prefilter.result.PreFilterScore;
import dev.lumenlang.lumen.pipeline.language.simulator.result.Suggestion;
import dev.lumenlang.lumen.pipeline.language.simulator.result.SuggestionIssue;
import dev.lumenlang.lumen.pipeline.language.simulator.sandbox.Sandbox;
import dev.lumenlang.lumen.pipeline.language.simulator.score.Confidence;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.typebinding.TypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Hot path for one pre-filtered candidate. Runs level-0 against the real matcher, retries with
 * one typo correction, falls back to partial-typo enrichment, and finally surfaces a
 * type-mismatch diagnostic from {@link MatchProgress}. Returns {@code null} when the candidate
 * cannot produce a useful suggestion.
 */
public final class TryMatch {

    private TryMatch() {
    }

    public static @Nullable Suggestion run(@NotNull List<Token> tokens, @NotNull PreFilterScore cs, @NotNull TypeRegistry types, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts, @NotNull SimulatorDebug debug) {
        Pattern pattern = cs.pattern();
        boolean dt = debug.enabled(Verbosity.DEEP_TIMING);
        List<LiteralInfo> literals = LiteralInfo.extract(pattern);
        double sandboxRejectedPenalty = opts.doubleValue(SimulatorOption.SANDBOX_REJECTED_PENALTY);
        Typo.Fix bestPartialTypo = null;
        MatchProgress bestPartialProgress = null;
        long lvl0Start = dt ? System.nanoTime() : 0L;
        MatchProgress level0Progress = PatternMatcher.matchWithProgress(tokens, pattern, types, env);
        if (dt) Trace.deepTiming(debug, "  level-0 match " + pattern.raw(), System.nanoTime() - lvl0Start);
        Trace.matchAttempt(debug, pattern, "level-0", level0Progress);
        if (level0Progress.succeeded()) {
            Throwable sandbox = level0Progress.match() != null ? Sandbox.run(cs.handler(), level0Progress.match(), env, pattern, "level-0", debug) : null;
            if (sandbox instanceof DiagnosticException de) {
                double confidence = Confidence.forTypo(0, true, opts) * sandboxRejectedPenalty;
                List<SuggestionIssue> issues = List.of(new SuggestionIssue.HandlerDiagnostic(de.diagnostic().title()));
                debug.trace(new TraceEvent.SuggestionFormed(pattern, confidence, "level-0 handler-diagnostic", issues));
                debug.emit(Verbosity.ISSUES, 2, () -> "level-0 syntactic match, handler rejected: " + de.diagnostic().title() + " (conf=" + String.format("%.3f", confidence) + ")");
                return new Suggestion(pattern, confidence, issues, level0Progress);
            }
            if (sandbox != null) {
                Trace.deep(debug, () -> "level-0 succeeded but sandbox rejected (non-diagnostic throw), abort candidate");
                return null;
            }
            debug.trace(new TraceEvent.SuggestionFormed(pattern, 1.0, "level-0 clean", List.of()));
            debug.emit(Verbosity.ISSUES, 2, () -> "level-0 clean match, conf=1.000");
            return new Suggestion(pattern, 1.0, List.of(), level0Progress);
        }
        long typoStart = dt ? System.nanoTime() : 0L;
        Typo.Fix typo = Typo.findBest(tokens, literals, pattern, debug);
        if (dt) Trace.deepTiming(debug, "  level-0 typo lookup " + pattern.raw(), System.nanoTime() - typoStart);
        if (typo != null) {
            long ctyStart = dt ? System.nanoTime() : 0L;
            List<Token> corrected = Typo.apply(tokens, typo);
            MatchProgress corrProgress = PatternMatcher.matchWithProgress(corrected, pattern, types, env);
            if (dt) Trace.deepTiming(debug, "  level-0 typo retry " + pattern.raw(), System.nanoTime() - ctyStart);
            Trace.matchAttempt(debug, pattern, "level-0 typo-corrected '" + typo.token().text() + "'->'" + typo.expected() + "'", corrProgress);
            boolean sandboxRejected = corrProgress.succeeded() && corrProgress.match() != null && !Sandbox.accepts(cs.handler(), corrProgress.match(), env, pattern, "level-0 typo", debug);
            debug.trace(new TraceEvent.TypoConsidered(pattern, typo.token(), typo.expected(), FuzzyMatch.prefixAwareDistance(typo.token().text(), typo.expected())));
            List<SuggestionIssue> typoIssues = List.of(new SuggestionIssue.Typo(typo.token(), typo.expected()));
            if (!sandboxRejected && corrProgress.succeeded()) {
                double confidence = Confidence.forTypo(1, true, opts);
                debug.trace(new TraceEvent.SuggestionFormed(pattern, confidence, "level-0 typo", typoIssues));
                debug.emit(Verbosity.ISSUES, 2, () -> "level-0 typo accepted, conf=" + String.format("%.3f", confidence) + " typo=" + typo.token().text() + "->" + typo.expected());
                return new Suggestion(pattern, confidence, typoIssues, corrProgress);
            }
            if (sandboxRejected && corrProgress.succeeded()) {
                double confidence = Confidence.forTypo(1, true, opts) * sandboxRejectedPenalty;
                debug.trace(new TraceEvent.SuggestionFormed(pattern, confidence, "level-0 typo (sandbox-penalised)", typoIssues));
                debug.emit(Verbosity.ISSUES, 2, () -> "level-0 typo accepted but sandbox penalised, conf=" + String.format("%.3f", confidence));
                return new Suggestion(pattern, confidence, typoIssues, corrProgress);
            }
            if (corrProgress.furthestTokenIndex() >= level0Progress.furthestTokenIndex()) {
                bestPartialTypo = typo;
                bestPartialProgress = corrProgress;
            }
        }
        if (bestPartialTypo != null) {
            if (bestPartialProgress.furthestTokenIndex() <= bestPartialTypo.tokenIndex() + 1 && bestPartialProgress.failedBindingId() == null && bestPartialProgress.bindingFailures().isEmpty()) {
                int furthest = bestPartialProgress.furthestTokenIndex();
                int typoIdx = bestPartialTypo.tokenIndex();
                Trace.deep(debug, () -> "partial-typo discarded: shallow progress (furthest=" + furthest + ", typoIdx=" + typoIdx + ", no binding failures)");
                bestPartialTypo = null;
                bestPartialProgress = null;
            }
        }
        if (bestPartialTypo != null) {
            if (cs.handler() != null) {
                List<Token> corrected = Typo.apply(tokens, bestPartialTypo);
                MatchProgress check = PatternMatcher.matchWithProgress(corrected, pattern, types, env);
                Trace.matchAttempt(debug, pattern, "partial-typo sandbox recheck", check);
                if (check.succeeded() && check.match() != null && !Sandbox.accepts(cs.handler(), check.match(), env, pattern, "partial-typo recheck", debug)) {
                    Trace.deep(debug, () -> "partial-typo sandbox rejected, discard");
                    bestPartialTypo = null;
                    bestPartialProgress = null;
                }
            }
        }
        if (bestPartialTypo != null) {
            Typo.Fix primaryTypo = bestPartialTypo;
            List<SuggestionIssue> issues = new ArrayList<>();
            issues.add(new SuggestionIssue.Typo(primaryTypo.token(), primaryTypo.expected()));
            if (!bestPartialProgress.bindingFailures().isEmpty()) {
                for (MatchProgress.BindingFailure bf : bestPartialProgress.bindingFailures()) {
                    if (!bf.failedTokens().isEmpty()) {
                        issues.add(new SuggestionIssue.TypeMismatch(bf.failedTokens().get(0), bf.bindingId(), bf.reason()));
                    } else {
                        issues.add(new SuggestionIssue.MissingBinding(bf.bindingId(), Fallback.missingBindingColumn(pattern, bf.bindingId(), null, tokens)));
                    }
                }
            } else if (bestPartialProgress.failedBindingId() != null && !bestPartialProgress.failedTokens().isEmpty()) {
                Token ft = bestPartialProgress.failedTokens().get(0);
                String reason = bestPartialProgress.failedReason();
                if (reason != null)
                    issues.add(new SuggestionIssue.TypeMismatch(ft, bestPartialProgress.failedBindingId(), reason));
            }
            for (MatchProgress.LiteralTypo lt : bestPartialProgress.literalTypos()) {
                if (!lt.token().text().equals(primaryTypo.token().text())) {
                    issues.add(new SuggestionIssue.Typo(lt.token(), lt.expected()));
                }
            }
            List<Token> correctedForFirstCheck = Typo.apply(tokens, primaryTypo);
            boolean firstMatch = Typo.firstTokenMatches(tokens, literals) || Typo.firstTokenMatches(correctedForFirstCheck, literals) || Typo.isFirstLiteralToken(primaryTypo, tokens, literals);
            int totalTypos = 1 + (int) bestPartialProgress.literalTypos().stream().filter(lt -> !lt.token().text().equals(primaryTypo.token().text())).count();
            double confidence = Math.min(Confidence.forTypo(totalTypos, firstMatch, opts), Confidence.forTypeMatch(bestPartialProgress, tokens.size()));
            List<SuggestionIssue> frozen = List.copyOf(issues);
            debug.trace(new TraceEvent.SuggestionFormed(pattern, confidence, "partial-typo fallback", frozen));
            debug.emit(Verbosity.ISSUES, 2, () -> "partial-typo fallback, conf=" + String.format("%.3f", confidence) + " issues=" + frozen.size());
            return new Suggestion(pattern, confidence, frozen, bestPartialProgress);
        }
        boolean hasIncomplete = level0Progress.incomplete() != null;
        if (level0Progress.failedBindingId() != null || !level0Progress.bindingFailures().isEmpty() || hasIncomplete) {
            List<SuggestionIssue> issues = new ArrayList<>();
            Typo.Fix heuristicTypo = Typo.findBest(tokens, literals, pattern, debug);
            if (heuristicTypo != null && FuzzyMatch.prefixAwareDistance(heuristicTypo.token().text(), heuristicTypo.expected()) <= 1) {
                issues.add(new SuggestionIssue.Typo(heuristicTypo.token(), heuristicTypo.expected()));
            }
            if (!level0Progress.bindingFailures().isEmpty()) {
                for (MatchProgress.BindingFailure bf : level0Progress.bindingFailures()) {
                    Token failed = bf.failedTokens().isEmpty() ? null : bf.failedTokens().get(0);
                    if (failed == null || Fallback.tokenIsLaterPatternLiteral(failed, literals)) {
                        issues.add(new SuggestionIssue.MissingBinding(bf.bindingId(), Fallback.missingBindingColumn(pattern, bf.bindingId(), failed, tokens)));
                    } else {
                        issues.add(new SuggestionIssue.TypeMismatch(failed, bf.bindingId(), bf.reason()));
                    }
                }
            } else {
                Token failedToken = level0Progress.failedTokens().isEmpty() ? null : level0Progress.failedTokens().get(0);
                String reason = level0Progress.failedReason();
                String bindingId = level0Progress.failedBindingId();
                if (failedToken != null && bindingId != null && reason != null) {
                    if (Fallback.tokenIsLaterPatternLiteral(failedToken, literals)) {
                        issues.add(new SuggestionIssue.MissingBinding(bindingId, Fallback.missingBindingColumn(pattern, bindingId, failedToken, tokens)));
                    } else {
                        issues.add(new SuggestionIssue.TypeMismatch(failedToken, bindingId, reason));
                    }
                }
            }
            if (level0Progress.incomplete() != null) {
                MatchProgress.Incomplete inc = level0Progress.incomplete();
                if (inc.afterTokenIndex() >= tokens.size()) {
                    issues.add(new SuggestionIssue.IncompleteInput(inc.expectedNext()));
                } else {
                    issues.add(new SuggestionIssue.MissingLiteral(inc.expectedNext(), inc.afterTokenIndex() - 1));
                }
            }
            double confidence = Confidence.forTypeMatch(level0Progress, tokens.size());
            List<SuggestionIssue> frozen = List.copyOf(issues);
            boolean missingLiteralOnly = !frozen.isEmpty() && frozen.stream().allMatch(i -> i instanceof SuggestionIssue.MissingLiteral);
            if (missingLiteralOnly) {
                double prefilterFloor = opts.doubleValue(SimulatorOption.MISSING_LITERAL_PREFILTER_FLOOR);
                if (cs.confidence() < prefilterFloor) {
                    Trace.deep(debug, () -> "MissingLiteral fallback dropped, preFilter " + String.format("%.3f", cs.confidence()) + " < floor " + String.format("%.3f", prefilterFloor));
                    return null;
                }
                confidence *= opts.doubleValue(SimulatorOption.MISSING_LITERAL_CONFIDENCE_FACTOR);
            }
            double finalConfidence = confidence;
            debug.trace(new TraceEvent.SuggestionFormed(pattern, finalConfidence, "type-mismatch fallback", frozen));
            debug.emit(Verbosity.ISSUES, 2, () -> "type-mismatch fallback, conf=" + String.format("%.3f", finalConfidence) + " issues=" + frozen.size());
            return new Suggestion(pattern, finalConfidence, frozen, level0Progress);
        }
        return null;
    }
}
