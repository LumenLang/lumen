package dev.lumenlang.lumen.pipeline.language.simulator;

import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.pattern.PatternMeta;
import dev.lumenlang.lumen.api.util.FuzzyMatch;
import dev.lumenlang.lumen.pipeline.codegen.BlockContextImpl;
import dev.lumenlang.lumen.pipeline.codegen.CodegenContextImpl;
import dev.lumenlang.lumen.pipeline.codegen.HandlerContextImpl;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.codegen.output.NoOpJavaOutput;
import dev.lumenlang.lumen.pipeline.conditions.registry.RegisteredCondition;
import dev.lumenlang.lumen.pipeline.language.match.Match;
import dev.lumenlang.lumen.pipeline.language.match.MatchProgress;
import dev.lumenlang.lumen.pipeline.language.match.PatternMatcher;
import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternPart;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredBlock;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredExpression;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredPattern;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.ScoreBreakdown;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.SimulatorDebug;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.Verbosity;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.trace.TraceEvent;
import dev.lumenlang.lumen.pipeline.language.simulator.options.SimulatorOption;
import dev.lumenlang.lumen.pipeline.language.simulator.options.SimulatorOptions;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.language.tokenization.TokenKind;
import dev.lumenlang.lumen.pipeline.typebinding.TypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Finds near matches when input tokens fail all normal matching paths.
 *
 * <p>This class is invoked after a statement, expression, condition, or block fails to match
 * any registered pattern through the normal pipeline. It uses a multi-pass approach:
 *
 * <ol>
 *   <li><b>Pre-filter</b>: scores all registered patterns against input tokens using fuzzy
 *       literal matching with prefix-aware distance. Keeps the top N candidates.</li>
 *   <li><b>BFS token removal</b>: for each candidate, tries removing 0 to k tokens from the
 *       input and matching against the real pattern matcher. At each removal level, also
 *       attempts single typo correction on the remaining tokens.</li>
 *   <li><b>Reorder fallback</b>: if BFS fails, attempts to rearrange tokens to match the
 *       pattern's expected order.</li>
 *   <li><b>Type mismatch fallback</b>: if all else fails, uses the match progress from
 *       level 0 to diagnose type binding failures.</li>
 * </ol>
 *
 * <h2>Configuration</h2>
 * <p>Tunable via {@link SimulatorOptions}. Each {@code suggest*} method exposes an
 * overload accepting custom options; the no-options form uses {@link SimulatorOptions#defaults()}.
 */
public final class PatternSimulator {

    private PatternSimulator() {
    }

    private static @NotNull String format(double v) {
        return String.format("%.3f", v);
    }

    private static int effectiveMaxK(int tokenCount, @NotNull SimulatorOptions opts) {
        int base = opts.intValue(SimulatorOption.MAX_REMOVAL_DEPTH);
        if (tokenCount > 25) return Math.min(base, 1);
        if (tokenCount > 20) return Math.min(base, 2);
        return base;
    }

    /**
     * Finds the closest matching expression patterns for unrecognized input tokens.
     *
     * @param tokens the tokens that failed expression matching
     * @param reg    the pattern registry
     * @param env    the type environment
     */
    public static @NotNull List<Suggestion> suggestExpressions(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env) {
        return suggestExpressions(tokens, reg, env, SimulatorOptions.defaults());
    }

    /**
     * Finds the closest matching expression patterns for unrecognized input tokens
     * using custom simulator options.
     *
     * @param tokens the tokens that failed expression matching
     * @param reg    the pattern registry
     * @param env    the type environment
     * @param opts   the simulator options to apply
     */
    public static @NotNull List<Suggestion> suggestExpressions(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts) {
        return suggestExpressions(tokens, reg, env, opts, SimulatorDebug.OFF);
    }

    /**
     * Finds the closest matching expression patterns with custom options and debug capture.
     *
     * @param tokens the tokens that failed expression matching
     * @param reg    the pattern registry
     * @param env    the type environment
     * @param opts   the simulator options to apply
     * @param debug  debug bag controlling verbosity, sink, and tracer
     */
    public static @NotNull List<Suggestion> suggestExpressions(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts, @NotNull SimulatorDebug debug) {
        if (tokens.isEmpty()) return List.of();
        debug.emit(Verbosity.RESULT, 0, () -> "suggestExpressions, " + tokens.size() + " input tokens");
        List<PreFilterScore> scored = new ArrayList<>();
        for (RegisteredExpression re : reg.getExpressions()) {
            PreFilterScore pfs = preFilter(tokens, re.pattern(), re.meta(), re, opts, debug);
            if (pfs != null) scored.add(pfs);
        }
        return analyze(scored, tokens, reg.getTypeRegistry(), env, opts, debug);
    }

    /**
     * Finds the closest matching condition patterns for unrecognized input tokens.
     *
     * @param tokens the tokens that failed condition matching
     * @param reg    the pattern registry
     * @param env    the type environment
     */
    public static @NotNull List<Suggestion> suggestConditions(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env) {
        return suggestConditions(tokens, reg, env, SimulatorOptions.defaults());
    }

    /**
     * Finds the closest matching condition patterns for unrecognized input tokens
     * using custom simulator options.
     *
     * @param tokens the tokens that failed condition matching
     * @param reg    the pattern registry
     * @param env    the type environment
     * @param opts   the simulator options to apply
     */
    public static @NotNull List<Suggestion> suggestConditions(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts) {
        return suggestConditions(tokens, reg, env, opts, SimulatorDebug.OFF);
    }

    /**
     * Finds the closest matching condition patterns with custom options and debug capture.
     *
     * @param tokens the tokens that failed condition matching
     * @param reg    the pattern registry
     * @param env    the type environment
     * @param opts   the simulator options to apply
     * @param debug  debug bag controlling verbosity, sink, and tracer
     */
    public static @NotNull List<Suggestion> suggestConditions(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts, @NotNull SimulatorDebug debug) {
        if (tokens.isEmpty()) return List.of();
        debug.emit(Verbosity.RESULT, 0, () -> "suggestConditions, " + tokens.size() + " input tokens");
        List<PreFilterScore> scored = new ArrayList<>();
        for (RegisteredCondition rc : reg.getConditionRegistry().getConditions()) {
            PreFilterScore pfs = preFilter(tokens, rc.pattern(), rc.meta(), null, opts, debug);
            if (pfs != null) scored.add(pfs);
        }
        return analyze(scored, tokens, reg.getTypeRegistry(), env, opts, debug);
    }

    /**
     * Finds the closest matching block patterns for unrecognized input tokens.
     *
     * @param tokens the tokens that failed block matching
     * @param reg    the pattern registry
     * @param env    the type environment
     */
    public static @NotNull List<Suggestion> suggestBlocks(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env) {
        return suggestBlocks(tokens, reg, env, SimulatorOptions.defaults());
    }

    /**
     * Finds the closest matching block patterns for unrecognized input tokens
     * using custom simulator options.
     *
     * @param tokens the tokens that failed block matching
     * @param reg    the pattern registry
     * @param env    the type environment
     * @param opts   the simulator options to apply
     */
    public static @NotNull List<Suggestion> suggestBlocks(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts) {
        return suggestBlocks(tokens, reg, env, opts, SimulatorDebug.OFF);
    }

    /**
     * Finds the closest matching block patterns with custom options and debug capture.
     *
     * @param tokens the tokens that failed block matching
     * @param reg    the pattern registry
     * @param env    the type environment
     * @param opts   the simulator options to apply
     * @param debug  debug bag controlling verbosity, sink, and tracer
     */
    public static @NotNull List<Suggestion> suggestBlocks(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts, @NotNull SimulatorDebug debug) {
        if (tokens.isEmpty()) return List.of();
        debug.emit(Verbosity.RESULT, 0, () -> "suggestBlocks, " + tokens.size() + " input tokens");
        List<PreFilterScore> scored = new ArrayList<>();
        for (RegisteredBlock rb : reg.getBlocks()) {
            PreFilterScore pfs = preFilter(tokens, rb.pattern(), rb.meta(), null, opts, debug);
            if (pfs != null) scored.add(pfs);
        }
        return analyze(scored, tokens, reg.getTypeRegistry(), env, opts, debug);
    }

    /**
     * Finds the closest matching patterns across both statements and expressions.
     *
     * @param tokens the tokens that failed all matching
     * @param reg    the pattern registry
     * @param env    the type environment
     */
    public static @NotNull List<Suggestion> suggestStatementsAndExpressions(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env) {
        return suggestStatementsAndExpressions(tokens, reg, env, SimulatorOptions.defaults());
    }

    /**
     * Finds the closest matching patterns across both statements and expressions
     * using custom simulator options.
     *
     * @param tokens the tokens that failed all matching
     * @param reg    the pattern registry
     * @param env    the type environment
     * @param opts   the simulator options to apply
     */
    public static @NotNull List<Suggestion> suggestStatementsAndExpressions(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts) {
        return suggestStatementsAndExpressions(tokens, reg, env, opts, SimulatorDebug.OFF);
    }

    /**
     * Finds the closest matching patterns across statements and expressions with custom options
     * and debug capture.
     *
     * @param tokens the tokens that failed all matching
     * @param reg    the pattern registry
     * @param env    the type environment
     * @param opts   the simulator options to apply
     * @param debug  debug bag controlling verbosity, sink, and tracer
     */
    public static @NotNull List<Suggestion> suggestStatementsAndExpressions(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts, @NotNull SimulatorDebug debug) {
        if (tokens.isEmpty()) return List.of();
        debug.emit(Verbosity.RESULT, 0, () -> "suggestStatementsAndExpressions, " + tokens.size() + " input tokens");
        List<PreFilterScore> scored = new ArrayList<>();
        for (RegisteredPattern rp : reg.getStatements()) {
            PreFilterScore pfs = preFilter(tokens, rp.pattern(), rp.meta(), rp, opts, debug);
            if (pfs != null) scored.add(pfs);
        }
        for (RegisteredExpression re : reg.getExpressions()) {
            PreFilterScore pfs = preFilter(tokens, re.pattern(), re.meta(), re, opts, debug);
            if (pfs != null) scored.add(pfs);
        }
        return analyze(scored, tokens, reg.getTypeRegistry(), env, opts, debug);
    }

    private static @NotNull List<Suggestion> analyze(@NotNull List<PreFilterScore> scored, @NotNull List<Token> tokens, @NotNull TypeRegistry types, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts, @NotNull SimulatorDebug debug) {
        long start = debug.enabled(Verbosity.TIMING) ? System.nanoTime() : 0L;
        scored.sort(Comparator.comparingDouble((PreFilterScore p) -> p.confidence).reversed());
        int limit = Math.min(opts.intValue(SimulatorOption.MAX_CANDIDATES), scored.size());
        debug.emit(Verbosity.CANDIDATES, 0, () -> "analyze " + scored.size() + " pre-filtered candidates, taking top " + limit);
        Map<Pattern, Suggestion> best = new LinkedHashMap<>();
        for (int i = 0; i < limit; i++) {
            PreFilterScore cs = scored.get(i);
            debug.emit(Verbosity.SCORED, 1, () -> "analyse #" + cs.pattern.raw() + " (preFilter=" + format(cs.confidence) + ")");
            long candStart = debug.enabled(Verbosity.TIMING) ? System.nanoTime() : 0L;
            Suggestion s = tryMatch(tokens, cs, types, env, opts, debug);
            if (debug.enabled(Verbosity.TIMING)) {
                Trace.timing(debug, "tryMatch " + cs.pattern.raw(), System.nanoTime() - candStart);
            }
            if (s == null) {
                debug.emit(Verbosity.SCORED, 2, () -> "rejected, no viable suggestion produced");
                continue;
            }
            Suggestion existing = best.get(s.pattern());
            if (existing == null || s.confidence() > existing.confidence()) {
                best.put(s.pattern(), s);
            }
        }
        List<Suggestion> results = new ArrayList<>(best.values());
        results.sort(Comparator.comparingDouble(Suggestion::confidence).reversed());
        int max = Math.min(opts.intValue(SimulatorOption.MAX_SUGGESTIONS), results.size());
        List<Suggestion> ordered = List.copyOf(results.subList(0, max));
        debug.trace(new TraceEvent.Ranked(ordered));
        if (debug.enabled(Verbosity.RANKED)) {
            debug.emit(Verbosity.RANKED, 0, () -> "ranked " + ordered.size() + " suggestion(s)");
            for (int i = 0; i < ordered.size(); i++) {
                Suggestion s = ordered.get(i);
                int rank = i;
                debug.emit(Verbosity.RANKED, 1, () -> "#" + rank + " " + format(s.confidence()) + "  " + s.pattern().raw());
            }
        }
        if (debug.enabled(Verbosity.TIMING)) {
            Trace.timing(debug, "analyze (total)", System.nanoTime() - start);
        }
        return ordered;
    }

    private static @Nullable Suggestion tryMatch(@NotNull List<Token> tokens, @NotNull PreFilterScore cs, @NotNull TypeRegistry types, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts, @NotNull SimulatorDebug debug) {
        Pattern pattern = cs.pattern;
        List<LiteralInfo> literals = extractLiterals(pattern);
        int maxK = Math.min(effectiveMaxK(tokens.size(), opts), tokens.size() - 1);
        int maxCombosPerLevel = opts.intValue(SimulatorOption.MAX_COMBINATIONS_PER_LEVEL);
        double sandboxRejectedPenalty = opts.doubleValue(SimulatorOption.SANDBOX_REJECTED_PENALTY);
        MatchProgress level0Progress = null;
        TypoFix bestPartialTypo = null;
        MatchProgress bestPartialProgress = null;
        for (int k = 0; k <= maxK; k++) {
            if (k == 0) {
                MatchProgress progress = PatternMatcher.matchWithProgress(tokens, pattern, types, env);
                Trace.matchAttempt(debug, pattern, "level-0", progress);
                level0Progress = progress;
                if (progress.succeeded()) {
                    Throwable sandbox = isBuiltin(cs.meta) && progress.match() != null ? runSandbox(cs.handler, progress.match(), env, pattern, "level-0", debug) : null;
                    if (sandbox instanceof DiagnosticException de) {
                        double confidence = computeConfidence(0, 0, true, opts) * sandboxRejectedPenalty;
                        List<SuggestionIssue> issues = List.of(new SuggestionIssue.HandlerDiagnostic(de.diagnostic().title()));
                        debug.trace(new TraceEvent.SuggestionFormed(pattern, confidence, "level-0 handler-diagnostic", issues));
                        debug.emit(Verbosity.ISSUES, 2, () -> "level-0 syntactic match, handler rejected: " + de.diagnostic().title() + " (conf=" + format(confidence) + ")");
                        return new Suggestion(pattern, confidence, issues, progress);
                    }
                    if (sandbox != null) {
                        Trace.deep(debug, 3, () -> "level-0 succeeded but sandbox rejected (non-diagnostic throw), abort candidate");
                        return null;
                    }
                    debug.trace(new TraceEvent.SuggestionFormed(pattern, 1.0, "level-0 clean", List.of()));
                    debug.emit(Verbosity.ISSUES, 2, () -> "level-0 clean match, conf=1.000");
                    return new Suggestion(pattern, 1.0, List.of(), progress);
                }
                TypoFix typo = findBestTypoFix(tokens, literals, pattern, debug);
                if (typo != null) {
                    List<Token> corrected = applyTypoFix(tokens, typo);
                    MatchProgress corrProgress = PatternMatcher.matchWithProgress(corrected, pattern, types, env);
                    Trace.matchAttempt(debug, pattern, "level-0 typo-corrected '" + typo.token.text() + "'->'" + typo.expected + "'", corrProgress);
                    boolean sandboxRejected = false;
                    if (corrProgress.succeeded()) {
                        if (isBuiltin(cs.meta) && corrProgress.match() != null && !tryHandlerSandbox(cs.handler, corrProgress.match(), env, pattern, "level-0 typo", debug)) {
                            sandboxRejected = true;
                        }
                    }
                    debug.trace(new TraceEvent.TypoConsidered(pattern, typo.token, typo.expected, FuzzyMatch.prefixAwareDistance(typo.token.text(), typo.expected)));
                    if (!sandboxRejected && corrProgress.succeeded()) {
                        double confidence = computeConfidence(0, 1, true, opts);
                        List<SuggestionIssue> issues = List.of(new SuggestionIssue.Typo(typo.token, typo.expected));
                        debug.trace(new TraceEvent.SuggestionFormed(pattern, confidence, "level-0 typo", issues));
                        debug.emit(Verbosity.ISSUES, 2, () -> "level-0 typo accepted, conf=" + format(confidence) + " typo=" + typo.token.text() + "->" + typo.expected);
                        return new Suggestion(pattern, confidence, issues, corrProgress);
                    }
                    if (sandboxRejected && corrProgress.succeeded()) {
                        double confidence = computeConfidence(0, 1, true, opts) * sandboxRejectedPenalty;
                        List<SuggestionIssue> issues = List.of(new SuggestionIssue.Typo(typo.token, typo.expected));
                        debug.trace(new TraceEvent.SuggestionFormed(pattern, confidence, "level-0 typo (sandbox-penalised)", issues));
                        debug.emit(Verbosity.ISSUES, 2, () -> "level-0 typo accepted but sandbox penalised, conf=" + format(confidence));
                        return new Suggestion(pattern, confidence, issues, corrProgress);
                    }
                    if (corrProgress.furthestTokenIndex() >= progress.furthestTokenIndex()) {
                        bestPartialTypo = typo;
                        bestPartialProgress = corrProgress;
                    }
                }
                continue;
            }
            List<Suggestion> levelResults = new ArrayList<>();
            int[] combo = new int[k];
            for (int ci = 0; ci < k; ci++) combo[ci] = ci;
            int combinationsChecked = 0;
            do {
                if (combinationsChecked++ >= maxCombosPerLevel) break;
                List<Token> reduced = removeIndices(tokens, combo);
                List<Token> removed = extractIndices(tokens, combo);
                MatchProgress progress = PatternMatcher.matchWithProgress(reduced, pattern, types, env);
                Trace.matchAttempt(debug, pattern, "BFS-" + k + " reduced", progress);
                Trace.bfsCombination(debug, pattern, k, combo.clone(), progress.succeeded(), progress.furthestTokenIndex());
                if (progress.succeeded()) {
                    int kx = k;
                    if (isBuiltin(cs.meta) && progress.match() != null && !tryHandlerSandbox(cs.handler, progress.match(), env, pattern, "BFS-" + kx + " extra", debug)) {
                        Trace.deep(debug, 4, () -> "BFS-" + kx + " match passed but sandbox rejected");
                        progress = null;
                    }
                }
                if (progress != null && progress.succeeded()) {
                    boolean firstMatch = firstTokenMatches(tokens, literals) || firstTokenMatches(reduced, literals);
                    double confidence = computeConfidence(k, 0, firstMatch, opts);
                    List<SuggestionIssue> issues = List.of(new SuggestionIssue.ExtraTokens(List.copyOf(removed)));
                    int level = k;
                    debug.trace(new TraceEvent.SuggestionFormed(pattern, confidence, "BFS-" + level + " extra", issues));
                    debug.emit(Verbosity.ISSUES, 2, () -> "BFS-" + level + " extra accepted, conf=" + format(confidence) + " removed=" + removed.size() + " tok(s)");
                    levelResults.add(new Suggestion(pattern, confidence, issues, progress));
                    continue;
                }
                TypoFix typo = findBestTypoFix(reduced, literals, pattern, debug);
                if (typo != null) {
                    List<Token> corrected = applyTypoFix(reduced, typo);
                    MatchProgress corrProgress = PatternMatcher.matchWithProgress(corrected, pattern, types, env);
                    Trace.matchAttempt(debug, pattern, "BFS-" + k + " typo-corrected '" + typo.token.text() + "'->'" + typo.expected + "'", corrProgress);
                    if (corrProgress.succeeded()) {
                        int kx = k;
                        if (isBuiltin(cs.meta) && corrProgress.match() != null && !tryHandlerSandbox(cs.handler, corrProgress.match(), env, pattern, "BFS-" + kx + " extra+typo", debug)) {
                            Trace.deep(debug, 4, () -> "BFS-" + kx + " typo-corrected match passed but sandbox rejected");
                            corrProgress = null;
                        }
                    }
                    if (corrProgress != null && corrProgress.succeeded()) {
                        double confidence = computeConfidence(k, 1, true, opts);
                        List<SuggestionIssue> issues = List.of(new SuggestionIssue.ExtraTokens(List.copyOf(removed)), new SuggestionIssue.Typo(typo.token, typo.expected));
                        int level = k;
                        debug.trace(new TraceEvent.TypoConsidered(pattern, typo.token, typo.expected, FuzzyMatch.prefixAwareDistance(typo.token.text(), typo.expected)));
                        debug.trace(new TraceEvent.SuggestionFormed(pattern, confidence, "BFS-" + level + " extra+typo", issues));
                        debug.emit(Verbosity.ISSUES, 2, () -> "BFS-" + level + " extra+typo accepted, conf=" + format(confidence));
                        levelResults.add(new Suggestion(pattern, confidence, issues, corrProgress));
                        continue;
                    }
                    if (corrProgress != null && corrProgress.furthestTokenIndex() > (bestPartialProgress != null ? bestPartialProgress.furthestTokenIndex() : -1)) {
                        bestPartialTypo = typo;
                        bestPartialProgress = corrProgress;
                    }
                }
            } while (nextCombination(combo, tokens.size()));
            int explored = combinationsChecked;
            int level = k;
            boolean any = !levelResults.isEmpty();
            debug.trace(new TraceEvent.BfsLevel(pattern, level, explored, any));
            debug.emit(Verbosity.MATCH, 2, () -> "BFS k=" + level + " explored=" + explored + " produced=" + (any ? "yes" : "no"));
            if (!levelResults.isEmpty()) {
                levelResults.sort(Comparator.comparingDouble(Suggestion::confidence).reversed().thenComparing(Comparator.comparingInt((Suggestion s) -> {
                    for (PatternSimulator.SuggestionIssue si : s.issues()) {
                        if (si instanceof SuggestionIssue.ExtraTokens extra) return extra.tokens().stream().mapToInt(Token::start).min().orElse(0);
                    }
                    return 0;
                }).reversed()));
                return levelResults.get(0);
            }
        }
        if (bestPartialTypo != null) {
            if (bestPartialProgress.furthestTokenIndex() <= bestPartialTypo.tokenIndex() + 1 && bestPartialProgress.failedBindingId() == null && bestPartialProgress.bindingFailures().isEmpty()) {
                int furthest = bestPartialProgress.furthestTokenIndex();
                int typoIdx = bestPartialTypo.tokenIndex();
                Trace.deep(debug, 3, () -> "partial-typo discarded: shallow progress (furthest=" + furthest + ", typoIdx=" + typoIdx + ", no binding failures)");
                bestPartialTypo = null;
                bestPartialProgress = null;
            }
        }
        if (bestPartialTypo != null) {
            if (isBuiltin(cs.meta) && cs.handler != null) {
                List<Token> corrected = applyTypoFix(tokens, bestPartialTypo);
                MatchProgress check = PatternMatcher.matchWithProgress(corrected, pattern, types, env);
                Trace.matchAttempt(debug, pattern, "partial-typo sandbox recheck", check);
                if (check.succeeded() && check.match() != null && !tryHandlerSandbox(cs.handler, check.match(), env, pattern, "partial-typo recheck", debug)) {
                    Trace.deep(debug, 3, () -> "partial-typo sandbox rejected, discard");
                    bestPartialTypo = null;
                    bestPartialProgress = null;
                }
            }
        }
        if (bestPartialTypo != null) {
            TypoFix primaryTypo = bestPartialTypo;
            List<SuggestionIssue> issues = new ArrayList<>();
            issues.add(new SuggestionIssue.Typo(primaryTypo.token, primaryTypo.expected));
            if (!bestPartialProgress.bindingFailures().isEmpty()) {
                for (MatchProgress.BindingFailure bf : bestPartialProgress.bindingFailures()) {
                    if (!bf.failedTokens().isEmpty()) {
                        issues.add(new SuggestionIssue.TypeMismatch(bf.failedTokens().get(0), bf.bindingId(), bf.reason()));
                    } else {
                        issues.add(new SuggestionIssue.MissingBinding(bf.bindingId()));
                    }
                }
            } else if (bestPartialProgress.failedBindingId() != null && !bestPartialProgress.failedTokens().isEmpty()) {
                Token ft = bestPartialProgress.failedTokens().get(0);
                String reason = bestPartialProgress.failedReason();
                if (reason != null)
                    issues.add(new SuggestionIssue.TypeMismatch(ft, bestPartialProgress.failedBindingId(), reason));
            }
            for (MatchProgress.LiteralTypo lt : bestPartialProgress.literalTypos()) {
                if (!lt.token().text().equals(primaryTypo.token.text())) {
                    issues.add(new SuggestionIssue.Typo(lt.token(), lt.expected()));
                }
            }
            if (!bestPartialProgress.unmatchedTrailingTokens().isEmpty()) {
                issues.add(new SuggestionIssue.ExtraTokens(bestPartialProgress.unmatchedTrailingTokens()));
            }
            List<Token> correctedForFirstCheck = applyTypoFix(tokens, primaryTypo);
            boolean firstMatch = firstTokenMatches(tokens, literals) || firstTokenMatches(correctedForFirstCheck, literals) || isFirstLiteralToken(primaryTypo, tokens, literals);
            int totalTypos = 1 + (int) bestPartialProgress.literalTypos().stream().filter(lt -> !lt.token().text().equals(primaryTypo.token.text())).count();
            double confidence = Math.min(computeConfidence(0, totalTypos, firstMatch, opts), computeTypeMatchConfidence(bestPartialProgress, tokens.size()));
            List<SuggestionIssue> frozen = List.copyOf(issues);
            debug.trace(new TraceEvent.SuggestionFormed(pattern, confidence, "partial-typo fallback", frozen));
            debug.emit(Verbosity.ISSUES, 2, () -> "partial-typo fallback, conf=" + format(confidence) + " issues=" + frozen.size());
            return new Suggestion(pattern, confidence, frozen, bestPartialProgress);
        }
        boolean hasIncomplete = level0Progress != null && level0Progress.incomplete() != null;
        if (level0Progress != null && (level0Progress.failedBindingId() != null || !level0Progress.bindingFailures().isEmpty() || hasIncomplete)) {
            List<SuggestionIssue> issues = new ArrayList<>();
            TypoFix heuristicTypo = findBestTypoFix(tokens, literals, pattern, debug);
            if (heuristicTypo != null && FuzzyMatch.prefixAwareDistance(heuristicTypo.token.text(), heuristicTypo.expected) <= 1) {
                issues.add(new SuggestionIssue.Typo(heuristicTypo.token, heuristicTypo.expected));
            }
            if (!level0Progress.bindingFailures().isEmpty()) {
                for (MatchProgress.BindingFailure bf : level0Progress.bindingFailures()) {
                    if (!bf.failedTokens().isEmpty()) {
                        issues.add(new SuggestionIssue.TypeMismatch(bf.failedTokens().get(0), bf.bindingId(), bf.reason()));
                    } else {
                        issues.add(new SuggestionIssue.MissingBinding(bf.bindingId()));
                    }
                }
            } else {
                Token failedToken = level0Progress.failedTokens().isEmpty() ? null : level0Progress.failedTokens().get(0);
                String reason = level0Progress.failedReason();
                String bindingId = level0Progress.failedBindingId();
                if (failedToken != null && bindingId != null && reason != null) {
                    issues.add(new SuggestionIssue.TypeMismatch(failedToken, bindingId, reason));
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
            if (!level0Progress.unmatchedTrailingTokens().isEmpty()) {
                issues.add(new SuggestionIssue.ExtraTokens(level0Progress.unmatchedTrailingTokens()));
            }
            double confidence = computeTypeMatchConfidence(level0Progress, tokens.size());
            List<SuggestionIssue> frozen = List.copyOf(issues);
            boolean missingLiteralOnly = !frozen.isEmpty() && frozen.stream().allMatch(i -> i instanceof SuggestionIssue.MissingLiteral);
            if (missingLiteralOnly) {
                double prefilterFloor = opts.doubleValue(SimulatorOption.MISSING_LITERAL_PREFILTER_FLOOR);
                if (cs.confidence < prefilterFloor) {
                    Trace.deep(debug, 3, () -> "MissingLiteral fallback dropped, preFilter " + format(cs.confidence) + " < floor " + format(prefilterFloor) + ", trying reorder");
                    return tryReorderMatch(tokens, cs, types, env, literals, opts, debug);
                }
                confidence *= opts.doubleValue(SimulatorOption.MISSING_LITERAL_CONFIDENCE_FACTOR);
                Suggestion reorderAlt = tryReorderMatch(tokens, cs, types, env, literals, opts, debug);
                if (reorderAlt != null && reorderAlt.confidence() > confidence) {
                    Trace.deep(debug, 3, () -> "reorder beat MissingLiteral, returning reorder");
                    return reorderAlt;
                }
            }
            double finalConfidence = confidence;
            debug.trace(new TraceEvent.SuggestionFormed(pattern, finalConfidence, "type-mismatch fallback", frozen));
            debug.emit(Verbosity.ISSUES, 2, () -> "type-mismatch fallback, conf=" + format(finalConfidence) + " issues=" + frozen.size());
            return new Suggestion(pattern, finalConfidence, frozen, level0Progress);
        }
        return tryReorderMatch(tokens, cs, types, env, literals, opts, debug);
    }

    private static @Nullable PreFilterScore preFilter(@NotNull List<Token> tokens, @NotNull Pattern pattern, @NotNull PatternMeta meta, @Nullable Object handler, @NotNull SimulatorOptions opts, @NotNull SimulatorDebug debug) {
        List<LiteralInfo> literals = extractLiterals(pattern);
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
                    int threshold = effectiveThreshold(tokens.get(j).text(), lit.primaryForm());
                    Trace.literalProbe(debug, pattern, j, tokens.get(j).text(), lit.primaryForm(), dist, threshold, dist <= threshold);
                }
                if (dist < bestDist) {
                    bestDist = dist;
                    bestIdx = j;
                }
            }
            int threshold = bestIdx >= 0 ? effectiveThreshold(tokens.get(bestIdx).text(), lit.primaryForm()) : 0;
            if (bestIdx >= 0 && bestDist <= threshold) {
                tokenUsed[bestIdx] = true;
                matches.add(new LiteralMatchResult(lit, bestIdx, bestDist));
            } else if (!lit.optional) {
                matches.add(new LiteralMatchResult(lit, -1, bestDist));
            }
        }
        int requiredCount = (int) literals.stream().filter(l -> !l.optional).count();
        if (requiredCount == 0) {
            Trace.preFilterReject(debug, pattern, "no required literals");
            return null;
        }
        int matchedRequired = (int) matches.stream().filter(m -> m.tokenIndex >= 0 && !m.literal.optional).count();
        if (matchedRequired == 0) {
            Trace.preFilterReject(debug, pattern, "no required literal matched (0/" + requiredCount + ")");
            return null;
        }
        int matchedTotal = (int) matches.stream().filter(m -> m.tokenIndex >= 0).count();
        int exactMatches = (int) matches.stream().filter(m -> m.tokenIndex >= 0 && m.distance == 0).count();
        double literalCoverage = matchedRequired / (double) requiredCount;
        double tokenCoverage = tokens.isEmpty() ? 0.0 : matchedTotal / (double) tokens.size();
        double exactness = matchedTotal > 0 ? exactMatches / (double) matchedTotal : 0.0;
        List<Integer> positions = matches.stream().filter(m -> m.tokenIndex >= 0).map(m -> m.tokenIndex).toList();
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
        double firstMultiplier = computeFirstTokenMultiplier(tokens, literals, matches, opts);
        double confidence = Math.min(1.0, base * firstMultiplier);
        double minConf = opts.doubleValue(SimulatorOption.MIN_PREFILTER_CONFIDENCE);
        boolean admitted = confidence >= minConf;
        ScoreBreakdown breakdown = new ScoreBreakdown(literalCoverage, exactness, positionAccuracy, tokenCoverage, firstMultiplier, base, confidence);
        debug.trace(new TraceEvent.CandidateScored(pattern, admitted, breakdown));
        debug.emit(Verbosity.BREAKDOWN, 1, () -> (admitted ? "+ " : "- ") + pattern.raw() + "  " + breakdown.oneLine());
        if (!admitted) {
            Trace.preFilterReject(debug, pattern, "confidence " + format(confidence) + " < MIN_PREFILTER " + format(minConf));
            return null;
        }
        return new PreFilterScore(pattern, confidence, matches, meta, handler);
    }

    private static int bestFormDistance(@NotNull String tokenText, @NotNull LiteralInfo lit) {
        int best = Integer.MAX_VALUE;
        for (String form : lit.forms) {
            int dist = simDistance(tokenText, form);
            if (dist < best) best = dist;
        }
        return best;
    }

    private static double computeFirstTokenMultiplier(@NotNull List<Token> tokens, @NotNull List<LiteralInfo> literals, @NotNull List<LiteralMatchResult> matches, @NotNull SimulatorOptions opts) {
        double miss = opts.doubleValue(SimulatorOption.FIRST_TOKEN_MISS_MULTIPLIER);
        LiteralInfo firstRequired = null;
        for (LiteralInfo lit : literals) {
            if (!lit.optional) {
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
            if (m.literal == firstRequired && m.tokenIndex == firstInputIdx) {
                return m.distance == 0 ? 1.0 : 0.85;
            }
        }
        return miss;
    }

    private static @Nullable TypoFix findBestTypoFix(@NotNull List<Token> tokens, @NotNull List<LiteralInfo> literals) {
        return findBestTypoFix(tokens, literals, null, SimulatorDebug.OFF);
    }

    private static @Nullable TypoFix findBestTypoFix(@NotNull List<Token> tokens, @NotNull List<LiteralInfo> literals, @Nullable Pattern pattern, @NotNull SimulatorDebug debug) {
        TypoFix best = null;
        int bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            for (LiteralInfo lit : literals) {
                for (String form : lit.forms) {
                    int dist = simDistance(token.text(), form);
                    int threshold = effectiveThreshold(token.text(), form);
                    boolean within = dist > 0 && dist <= threshold;
                    boolean kept = within && dist < bestDist;
                    if (debug.enabled(Verbosity.DEEP) && pattern != null) {
                        Trace.typoCandidate(debug, pattern, i, token.text(), form, dist, threshold, kept);
                    }
                    if (kept) {
                        bestDist = dist;
                        best = new TypoFix(token, form, i);
                    }
                }
            }
        }
        return best;
    }

    private static @NotNull List<Token> applyTypoFix(@NotNull List<Token> tokens, @NotNull TypoFix fix) {
        List<Token> result = new ArrayList<>(tokens);
        result.set(fix.tokenIndex, syntheticToken(fix.expected, tokens));
        return result;
    }

    private static boolean firstTokenMatches(@NotNull List<Token> tokens, @NotNull List<LiteralInfo> literals) {
        LiteralInfo firstRequired = null;
        for (LiteralInfo lit : literals) {
            if (!lit.optional) {
                firstRequired = lit;
                break;
            }
        }
        if (firstRequired == null) return false;
        Token firstInput = null;
        for (Token t : tokens) {
            if (t.kind() != TokenKind.SYMBOL) {
                firstInput = t;
                break;
            }
        }
        if (firstInput == null) return false;
        for (String form : firstRequired.forms) {
            if (form.equalsIgnoreCase(firstInput.text())) return true;
            if (FuzzyMatch.prefixAwareDistance(firstInput.text(), form) <= 1) return true;
        }
        return false;
    }

    private static boolean isFirstLiteralToken(@NotNull TypoFix typo, @NotNull List<Token> tokens, @NotNull List<LiteralInfo> literals) {
        LiteralInfo firstRequired = null;
        for (LiteralInfo lit : literals) {
            if (!lit.optional) {
                firstRequired = lit;
                break;
            }
        }
        if (firstRequired == null) return false;
        int firstInputIdx = -1;
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).kind() != TokenKind.SYMBOL) {
                firstInputIdx = i;
                break;
            }
        }
        if (firstInputIdx < 0) return false;
        return typo.tokenIndex == firstInputIdx && firstRequired.forms.stream().anyMatch(f -> f.equalsIgnoreCase(typo.expected));
    }

    private static double computeConfidence(int removals, int typos, boolean firstTokenMatches, @NotNull SimulatorOptions opts) {
        double base = 1.0 - (removals * opts.doubleValue(SimulatorOption.REMOVAL_PENALTY)) - (typos * opts.doubleValue(SimulatorOption.TYPO_PENALTY));
        if (!firstTokenMatches) base *= opts.doubleValue(SimulatorOption.FIRST_TOKEN_MISS_MULTIPLIER);
        return Math.max(0.0, Math.min(1.0, base));
    }

    private static double computeTypeMatchConfidence(@NotNull MatchProgress progress, int totalTokens) {
        double fraction = totalTokens > 0 ? (double) (progress.furthestTokenIndex() + 1) / totalTokens : 0.0;
        double base = Math.max(0.20, Math.min(0.85, fraction));
        int failures = progress.bindingFailures().size();
        double penalty = failures * 0.12;
        return Math.max(0.05, base - penalty);
    }

    private static int effectiveThreshold(@NotNull String tokenText, @NotNull String formText) {
        int tokenLen = tokenText.length();
        int formLen = formText.length();
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
     * Distance used internally by the simulator. For most inputs this delegates to
     * {@link FuzzyMatch#prefixAwareDistance}, but 2-char tokens fall back to plain
     * Damerau-Levenshtein because the prefix-penalty scheme inflates the distance for genuine
     * single-edit typos like {@code st} for {@code set}.
     */
    private static int simDistance(@NotNull String tokenText, @NotNull String formText) {
        if (tokenText.length() == 2 && formText.length() >= 2 && formText.length() <= 3) {
            return FuzzyMatch.damerauLevenshteinDistance(tokenText, formText);
        }
        return FuzzyMatch.prefixAwareDistance(tokenText, formText);
    }

    /**
     * {@code true} when every character in {@code token} (case folded, counting multiplicity)
     * appears in {@code form}.
     */
    private static boolean isCharBagSubset(@NotNull String token, @NotNull String form) {
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

    private static @NotNull List<Token> removeIndices(@NotNull List<Token> tokens, int[] indices) {
        boolean[] skip = new boolean[tokens.size()];
        for (int idx : indices) skip[idx] = true;
        List<Token> result = new ArrayList<>(tokens.size() - indices.length);
        for (int i = 0; i < tokens.size(); i++) {
            if (!skip[i]) result.add(tokens.get(i));
        }
        return result;
    }

    private static @NotNull List<Token> extractIndices(@NotNull List<Token> tokens, int[] indices) {
        List<Token> result = new ArrayList<>(indices.length);
        for (int idx : indices) result.add(tokens.get(idx));
        return result;
    }

    private static boolean nextCombination(int[] combo, int n) {
        int k = combo.length;
        int i = k - 1;
        while (i >= 0 && combo[i] == n - k + i) i--;
        if (i < 0) return false;
        combo[i]++;
        for (int j = i + 1; j < k; j++) combo[j] = combo[j - 1] + 1;
        return true;
    }

    private static @Nullable Suggestion tryReorderMatch(@NotNull List<Token> tokens, @NotNull PreFilterScore cs, @NotNull TypeRegistry types, @NotNull TypeEnvImpl env, @NotNull List<LiteralInfo> literals, @NotNull SimulatorOptions opts, @NotNull SimulatorDebug debug) {
        double prefilterFloor = opts.doubleValue(SimulatorOption.REORDER_PREFILTER_FLOOR);
        if (cs.confidence < prefilterFloor) {
            Trace.deep(debug, 2, () -> "reorder skipped: preFilter " + format(cs.confidence) + " < REORDER_PREFILTER_FLOOR " + format(prefilterFloor));
            return null;
        }
        List<LiteralMatchResult> matchDetails = cs.matchDetails;
        long anchoredCount = matchDetails.stream().filter(m -> m.tokenIndex >= 0).count();
        int shapeLimit = opts.intValue(SimulatorOption.SHAPE_MATCH_TOKEN_LIMIT);
        if (tokens.size() > shapeLimit) {
            Trace.deep(debug, 2, () -> "reorder skipped: " + tokens.size() + " tokens > SHAPE_MATCH_TOKEN_LIMIT " + shapeLimit);
            return null;
        }
        if (anchoredCount < 1) {
            Trace.deep(debug, 2, () -> "reorder skipped: no anchored literals");
            return null;
        }
        MatchProgress shaped = tryShapeMatch(tokens, cs.pattern, matchDetails, types, env, debug);
        if (shaped == null) {
            Trace.deep(debug, 2, () -> "reorder skipped: shape produced nothing");
            return null;
        }
        if (!shaped.succeeded()) {
            Trace.deep(debug, 2, () -> "reorder skipped: shaped sequence did not match (furthest=" + shaped.furthestTokenIndex() + ")");
            return null;
        }
        if (isBuiltin(cs.meta) && shaped.match() != null && !tryHandlerSandbox(cs.handler, shaped.match(), env, cs.pattern, "reorder shape-match", debug)) {
            Trace.deep(debug, 2, () -> "reorder skipped: shaped match passed but sandbox rejected");
            return null;
        }
        List<Token> reordered = findReorderedFromAnchors(tokens, matchDetails);
        if (reordered.isEmpty()) {
            Trace.deep(debug, 2, () -> "reorder skipped: no reordered tokens identified from anchors");
            return null;
        }
        boolean firstMatch = firstTokenMatches(tokens, literals);
        double confidence = Math.max(opts.doubleValue(SimulatorOption.VALIDATED_REORDER_FLOOR), computeConfidence(0, 0, firstMatch, opts));
        List<SuggestionIssue> issues = List.of(new SuggestionIssue.Reorder(reordered));
        debug.trace(new TraceEvent.SuggestionFormed(cs.pattern, confidence, "reorder fallback", issues));
        debug.emit(Verbosity.ISSUES, 2, () -> "reorder fallback, conf=" + format(confidence) + " reordered=" + reordered.size() + " tok(s)");
        return new Suggestion(cs.pattern, confidence, issues, shaped);
    }

    private static @Nullable MatchProgress tryShapeMatch(@NotNull List<Token> tokens, @NotNull Pattern pattern, @NotNull List<LiteralMatchResult> matchDetails, @NotNull TypeRegistry types, @NotNull TypeEnvImpl env, @NotNull SimulatorDebug debug) {
        List<LiteralMatchResult> anchored = matchDetails.stream().filter(m -> m.tokenIndex >= 0).sorted(Comparator.comparingInt(m -> m.literal.partIndex)).toList();
        if (anchored.isEmpty()) return null;
        boolean[] used = new boolean[tokens.size()];
        for (LiteralMatchResult m : anchored) {
            if (m.tokenIndex >= 0 && m.tokenIndex < tokens.size()) used[m.tokenIndex] = true;
        }
        List<Token> remaining = new ArrayList<>();
        for (int j = 0; j < tokens.size(); j++) {
            if (!used[j]) remaining.add(tokens.get(j));
        }
        List<PatternPart> parts = pattern.parts();
        List<Token> shaped = new ArrayList<>();
        int remIdx = 0;
        for (PatternPart part : parts) {
            if (part instanceof PatternPart.Literal lit) {
                Token anchor = findAnchorToken(anchored, tokens, lit.text());
                shaped.add(anchor != null ? anchor : syntheticToken(lit.text(), tokens));
            } else if (part instanceof PatternPart.FlexLiteral flex) {
                Token anchor = findFlexAnchorToken(anchored, tokens, flex.forms());
                shaped.add(anchor != null ? anchor : syntheticToken(flex.forms().get(0), tokens));
            } else if (part instanceof PatternPart.PlaceholderPart) {
                if (remIdx < remaining.size()) {
                    shaped.add(remaining.get(remIdx++));
                }
            } else if (part instanceof PatternPart.Group group) {
                if (!group.required()) continue;
                for (List<PatternPart> alt : group.alternatives()) {
                    for (PatternPart ap : alt) {
                        if (ap instanceof PatternPart.Literal gl) {
                            Token a = findAnchorToken(anchored, tokens, gl.text());
                            if (a != null) {
                                shaped.add(a);
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        }
        while (remIdx < remaining.size()) {
            shaped.add(remaining.get(remIdx++));
        }
        if (shaped.isEmpty()) {
            Trace.shapeAttempt(debug, pattern, List.of(), null);
            return null;
        }
        MatchProgress shapedProgress = PatternMatcher.matchWithProgress(shaped, pattern, types, env);
        Trace.shapeAttempt(debug, pattern, shaped, shapedProgress);
        Trace.matchAttempt(debug, pattern, "shape-match", shapedProgress);
        return shapedProgress;
    }

    private static @Nullable Token findAnchorToken(@NotNull List<LiteralMatchResult> anchored, @NotNull List<Token> tokens, @NotNull String text) {
        for (LiteralMatchResult m : anchored) {
            if (m.tokenIndex >= 0 && m.tokenIndex < tokens.size() && m.literal.forms.stream().anyMatch(f -> f.equalsIgnoreCase(text))) {
                return tokens.get(m.tokenIndex);
            }
        }
        return null;
    }

    private static @Nullable Token findFlexAnchorToken(@NotNull List<LiteralMatchResult> anchored, @NotNull List<Token> tokens, @NotNull List<String> forms) {
        for (String form : forms) {
            Token found = findAnchorToken(anchored, tokens, form);
            if (found != null) return found;
        }
        return null;
    }

    private static @NotNull List<Token> findReorderedFromAnchors(@NotNull List<Token> tokens, @NotNull List<LiteralMatchResult> matchDetails) {
        List<LiteralMatchResult> anchored = matchDetails.stream().filter(m -> m.tokenIndex >= 0).sorted(Comparator.comparingInt(m -> m.literal.partIndex)).toList();
        if (anchored.isEmpty()) return List.of();
        List<Token> result = new ArrayList<>();
        int expectedPos = 0;
        for (LiteralMatchResult m : anchored) {
            if (m.tokenIndex != expectedPos) {
                for (int j = Math.min(expectedPos, m.tokenIndex); j <= Math.max(expectedPos, m.tokenIndex) && j < tokens.size(); j++) {
                    if (!result.contains(tokens.get(j))) result.add(tokens.get(j));
                }
            }
            expectedPos = m.tokenIndex + 1;
        }
        return result.size() >= 2 ? List.copyOf(result) : List.of();
    }

    private static @NotNull Token syntheticToken(@NotNull String text, @NotNull List<Token> reference) {
        int line = reference.isEmpty() ? 1 : reference.get(0).line();
        return new Token(TokenKind.IDENT, text, line, 0, text.length());
    }

    private static boolean isBuiltin(@NotNull PatternMeta meta) {
        return "Lumen".equals(meta.by());
    }

    private static @Nullable Throwable runSandbox(@Nullable Object handler, @NotNull Match match, @NotNull TypeEnvImpl env, @NotNull Pattern pattern, @NotNull String stage, @NotNull SimulatorDebug debug) {
        Throwable thrown;
        if (handler instanceof RegisteredPattern rp) {
            thrown = tryStatementHandler(rp, match, env);
        } else if (handler instanceof RegisteredExpression re) {
            thrown = tryExpressionHandler(re, match, env);
        } else {
            return null;
        }
        if (thrown != null) Trace.sandboxRejected(debug, pattern, stage, thrown);
        return thrown;
    }

    private static boolean tryHandlerSandbox(@Nullable Object handler, @NotNull Match match, @NotNull TypeEnvImpl env, @NotNull Pattern pattern, @NotNull String stage, @NotNull SimulatorDebug debug) {
        return runSandbox(handler, match, env, pattern, stage, debug) == null;
    }

    /**
     * Attempts to run a statement handler in a sandboxed context to verify viability.
     *
     * @param handler the registered statement pattern
     * @param match   the match result from corrected token matching
     * @param env     the type environment
     * @return {@code null} if the handler executed without throwing, otherwise the throwable
     */
    public static @Nullable Throwable tryStatementHandler(@NotNull RegisteredPattern handler, @NotNull Match match, @NotNull TypeEnvImpl env) {
        try {
            CodegenContextImpl codegenCtx = new CodegenContextImpl("__simulation__.luma");
            BlockContextImpl blockCtx = sandboxBlock(env);
            HandlerContextImpl hctx = new HandlerContextImpl(match, env, codegenCtx, blockCtx, NoOpJavaOutput.INSTANCE);
            handler.handler().handle(hctx);
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    /**
     * Attempts to run an expression handler in a sandboxed context to verify viability.
     *
     * @param handler the registered expression pattern
     * @param match   the match result from corrected token matching
     * @param env     the type environment
     * @return {@code null} if the handler executed without throwing, otherwise the throwable
     */
    public static @Nullable Throwable tryExpressionHandler(@NotNull RegisteredExpression handler, @NotNull Match match, @NotNull TypeEnvImpl env) {
        try {
            CodegenContextImpl codegenCtx = new CodegenContextImpl("__simulation__.luma");
            BlockContextImpl blockCtx = sandboxBlock(env);
            HandlerContextImpl hctx = new HandlerContextImpl(match, env, codegenCtx, blockCtx, NoOpJavaOutput.INSTANCE);
            handler.handler().handle(hctx);
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    private static @NotNull BlockContextImpl sandboxBlock(@NotNull TypeEnvImpl env) {
        try {
            return env.blockContext();
        } catch (IllegalStateException missing) {
            return new BlockContextImpl(null, null, List.of(), 0);
        }
    }

    private static @NotNull List<LiteralInfo> extractLiterals(@NotNull Pattern pattern) {
        List<LiteralInfo> result = new ArrayList<>();
        extractLiteralsFromParts(pattern.parts(), result, false);
        return result;
    }

    private static void extractLiteralsFromParts(@NotNull List<PatternPart> parts, @NotNull List<LiteralInfo> result, boolean parentOptional) {
        int partIndex = result.size();
        for (PatternPart part : parts) {
            if (part instanceof PatternPart.Literal lit) {
                result.add(new LiteralInfo(List.of(lit.text()), partIndex++, parentOptional));
            } else if (part instanceof PatternPart.FlexLiteral flex) {
                result.add(new LiteralInfo(flex.forms(), partIndex++, parentOptional));
            } else if (part instanceof PatternPart.Group group) {
                if (!group.alternatives().isEmpty()) {
                    extractLiteralsFromParts(group.alternatives().get(0), result, !group.required() || parentOptional);
                }
                partIndex++;
            } else {
                partIndex++;
            }
        }
    }

    /**
     * A specific issue detected in the input tokens relative to a candidate pattern.
     */
    public sealed interface SuggestionIssue {

        /**
         * A token in the input that is a likely typo of a pattern literal.
         *
         * @param token    the input token containing the typo
         * @param expected the correct literal text
         */
        record Typo(@NotNull Token token, @NotNull String expected) implements SuggestionIssue {
        }

        /**
         * Tokens in the input that are not part of the matched pattern.
         *
         * @param tokens the extra tokens that should be removed
         */
        record ExtraTokens(@NotNull List<Token> tokens) implements SuggestionIssue {
        }

        /**
         * Tokens in the input that are in the wrong order relative to the pattern.
         *
         * @param tokens the tokens that need reordering
         */
        record Reorder(@NotNull List<Token> tokens) implements SuggestionIssue {
        }

        /**
         * A token that was rejected by a type binding in the pattern.
         *
         * @param token     the rejected input token
         * @param bindingId the type binding id that rejected it
         * @param reason    a human readable rejection reason produced by the binding
         */
        record TypeMismatch(@NotNull Token token, @NotNull String bindingId,
                            @NotNull String reason) implements SuggestionIssue {
        }

        /**
         * A type binding that expects input but received none (missing tokens).
         *
         * @param bindingId the type binding ID that is missing
         */
        record MissingBinding(@NotNull String bindingId) implements SuggestionIssue {
        }

        /**
         * The pattern's handler accepted the syntactic match but rejected it semantically by
         * throwing a {@link DiagnosticException}. The diagnostic title carries the underlying reason.
         *
         * @param title the diagnostic title from the thrown exception
         */
        record HandlerDiagnostic(@NotNull String title) implements SuggestionIssue {
        }

        /**
         * Pattern expected a literal keyword that the input never produced.
         *
         * @param literal       the literal text the pattern expected
         * @param afterTokenIndex token index after which the literal was missing, or {@code -1}
         *                       when the literal was missing from the very start
         */
        record MissingLiteral(@NotNull String literal, int afterTokenIndex) implements SuggestionIssue {
        }

        /**
         * Input ended while the pattern still expected more content.
         *
         * @param expectedNext short label naming what the pattern expected next (literal text or
         *                    binding id)
         */
        record IncompleteInput(@NotNull String expectedNext) implements SuggestionIssue {
        }
    }

    /**
     * A single simulation result describing a near match and the issues found.
     *
     * @param pattern    the candidate pattern that closely matched the input
     * @param confidence confidence score between 0.0 and 1.0 (0% to 100%)
     * @param issues     the specific issues detected (typos, extra tokens, reorders, type mismatches)
     * @param progress   match progress from real simulation, or null if not enriched
     */
    public record Suggestion(@NotNull Pattern pattern, double confidence, @NotNull List<SuggestionIssue> issues, @Nullable MatchProgress progress) {
    }

    private record PreFilterScore(@NotNull Pattern pattern, double confidence, @NotNull List<LiteralMatchResult> matchDetails, @NotNull PatternMeta meta, @Nullable Object handler) {
    }

    private record LiteralInfo(@NotNull List<String> forms, int partIndex, boolean optional) {
        @NotNull String primaryForm() {
            return forms.get(0);
        }
    }

    private record LiteralMatchResult(@NotNull LiteralInfo literal, int tokenIndex, int distance) {
    }

    private record TypoFix(@NotNull Token token, @NotNull String expected, int tokenIndex) {
    }

    /**
     * Internal helper centralising debug emission. Each method gates on verbosity once and emits
     * both a structured trace event and a formatted sink line.
     */
    private static final class Trace {

        private Trace() {
        }

        static void preFilterReject(@NotNull SimulatorDebug debug, @NotNull Pattern pattern, @NotNull String reason) {
            debug.trace(new TraceEvent.PreFilterRejected(pattern, reason));
            debug.emit(Verbosity.CANDIDATES, 1, () -> "- " + pattern.raw() + "  rejected: " + reason);
        }

        static void literalProbe(@NotNull SimulatorDebug debug, @NotNull Pattern pattern, int tokenIndex, @NotNull String tokenText, @NotNull String form, int distance, int threshold, boolean accepted) {
            if (!debug.enabled(Verbosity.DEEP)) return;
            debug.trace(new TraceEvent.LiteralProbe(pattern, tokenIndex, tokenText, form, distance, threshold, accepted));
            debug.emit(Verbosity.DEEP, 3, () -> "probe tok#" + tokenIndex + " '" + tokenText + "' vs form '" + form + "' dist=" + distance + " thr=" + threshold + " " + (accepted ? "ACCEPT" : "skip"));
        }

        static void typoCandidate(@NotNull SimulatorDebug debug, @NotNull Pattern pattern, int tokenIndex, @NotNull String tokenText, @NotNull String form, int distance, int threshold, boolean keptAsBest) {
            if (!debug.enabled(Verbosity.DEEP)) return;
            debug.trace(new TraceEvent.TypoCandidate(pattern, tokenIndex, tokenText, form, distance, threshold, keptAsBest));
            debug.emit(Verbosity.DEEP, 3, () -> "typo? tok#" + tokenIndex + " '" + tokenText + "' -> '" + form + "' dist=" + distance + " thr=" + threshold + (keptAsBest ? " *best*" : ""));
        }

        static void matchAttempt(@NotNull SimulatorDebug debug, @NotNull Pattern pattern, @NotNull String stage, @NotNull MatchProgress progress) {
            debug.trace(new TraceEvent.MatchAttempt(pattern, stage, progress));
            if (!debug.enabled(Verbosity.BIND)) return;
            debug.emit(Verbosity.BIND, 3, () -> stage + " match: " + (progress.succeeded() ? "OK" : "FAIL") + " furthest=" + progress.furthestTokenIndex() + (progress.failedBindingId() != null ? " failedBinding=" + progress.failedBindingId() : "") + (progress.failedReason() != null ? " reason=" + progress.failedReason() : ""));
            for (MatchProgress.BindingFailure bf : progress.bindingFailures()) {
                debug.emit(Verbosity.BIND, 4, () -> "binding " + bf.bindingId() + " failed: " + bf.reason() + " (failedTokens=" + bf.failedTokens().size() + ")");
            }
            for (MatchProgress.LiteralTypo lt : progress.literalTypos()) {
                debug.emit(Verbosity.BIND, 4, () -> "literalTypo: '" + lt.token().text() + "' expected '" + lt.expected() + "'");
            }
            if (!progress.unmatchedTrailingTokens().isEmpty()) {
                debug.emit(Verbosity.BIND, 4, () -> "unmatched trailing: " + progress.unmatchedTrailingTokens().size() + " tok(s)");
            }
        }

        static void bfsCombination(@NotNull SimulatorDebug debug, @NotNull Pattern pattern, int level, int[] removedIndices, boolean matched, int furthestIndex) {
            debug.trace(new TraceEvent.BfsCombination(pattern, level, removedIndices, matched, furthestIndex));
            if (!debug.enabled(Verbosity.MATCH)) return;
            StringBuilder ix = new StringBuilder("[");
            for (int i = 0; i < removedIndices.length; i++) {
                if (i > 0) ix.append(',');
                ix.append(removedIndices[i]);
            }
            ix.append(']');
            String ixStr = ix.toString();
            debug.emit(Verbosity.MATCH, 3, () -> "BFS-" + level + " remove=" + ixStr + " " + (matched ? "MATCH" : "no-match furthest=" + furthestIndex));
        }

        static void shapeAttempt(@NotNull SimulatorDebug debug, @NotNull Pattern pattern, @NotNull List<Token> shaped, @Nullable MatchProgress progress) {
            debug.trace(new TraceEvent.ShapeAttempt(pattern, shaped, progress));
            if (!debug.enabled(Verbosity.DEEP)) return;
            StringBuilder sb = new StringBuilder("shape=[");
            for (int i = 0; i < shaped.size(); i++) {
                if (i > 0) sb.append(' ');
                sb.append(shaped.get(i).text());
            }
            sb.append("] -> ").append(progress == null ? "null" : (progress.succeeded() ? "OK" : "FAIL furthest=" + progress.furthestTokenIndex()));
            String line = sb.toString();
            debug.emit(Verbosity.DEEP, 3, () -> line);
        }

        static void deep(@NotNull SimulatorDebug debug, int depth, @NotNull Supplier<String> line) {
            debug.emit(Verbosity.DEEP, depth, line);
        }

        static void timing(@NotNull SimulatorDebug debug, @NotNull String stage, long nanos) {
            long ms = nanos / 1_000_000L;
            debug.trace(new TraceEvent.StageTiming(stage, ms));
            debug.emit(Verbosity.TIMING, 1, () -> stage + " " + ms + " ms (" + nanos + " ns)");
        }

        static void sandboxRejected(@NotNull SimulatorDebug debug, @NotNull Pattern pattern, @NotNull String stage, @NotNull Throwable thrown) {
            debug.trace(new TraceEvent.SandboxRejected(pattern, stage, thrown));
            if (!debug.enabled(Verbosity.BIND)) return;
            String type = thrown.getClass().getSimpleName();
            String msg = thrown.getMessage();
            debug.emit(Verbosity.BIND, 4, () -> "sandbox rejected (" + stage + "): " + type + (msg == null ? "" : ": " + msg));
            if (debug.enabled(Verbosity.DEEP)) {
                StackTraceElement[] trace = thrown.getStackTrace();
                int shown = Math.min(trace.length, 6);
                for (int i = 0; i < shown; i++) {
                    StackTraceElement el = trace[i];
                    debug.emit(Verbosity.DEEP, 5, () -> "at " + el);
                }
            }
        }
    }
}
