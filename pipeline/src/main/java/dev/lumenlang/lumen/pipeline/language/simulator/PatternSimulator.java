package dev.lumenlang.lumen.pipeline.language.simulator;

import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.conditions.registry.RegisteredCondition;
import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredBlock;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredExpression;
import dev.lumenlang.lumen.pipeline.language.pattern.registered.RegisteredPattern;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.SimulatorDebug;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.Trace;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.Verbosity;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.trace.TraceEvent;
import dev.lumenlang.lumen.pipeline.language.simulator.options.SimulatorOption;
import dev.lumenlang.lumen.pipeline.language.simulator.options.SimulatorOptions;
import dev.lumenlang.lumen.pipeline.language.simulator.prefilter.PreFilter;
import dev.lumenlang.lumen.pipeline.language.simulator.prefilter.result.PreFilterScore;
import dev.lumenlang.lumen.pipeline.language.simulator.result.Suggestion;
import dev.lumenlang.lumen.pipeline.language.simulator.walk.TryMatch;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.typebinding.TypeRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds near matches when input tokens fail all normal matching paths.
 *
 * <p>Pipeline: pre-filter ranks every registered pattern by fuzzy literal coverage and keeps
 * the top N. Each survivor goes through a level-0 match, a single-token typo retry, a partial
 * typo fallback, and a type-mismatch fallback that surfaces structured binding failures.
 *
 * <p>Tunable via {@link SimulatorOptions}. Each {@code suggest*} method exposes an overload
 * accepting custom options; the no-options form uses {@link SimulatorOptions#defaults()}.
 */
public final class PatternSimulator {

    private PatternSimulator() {
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
     * Finds the closest matching expression patterns with custom simulator options.
     */
    public static @NotNull List<Suggestion> suggestExpressions(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts) {
        return suggestExpressions(tokens, reg, env, opts, SimulatorDebug.OFF);
    }

    /**
     * Finds the closest matching expression patterns with custom options and debug capture.
     */
    public static @NotNull List<Suggestion> suggestExpressions(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts, @NotNull SimulatorDebug debug) {
        if (tokens.isEmpty()) return List.of();
        debug.emit(Verbosity.RESULT, 0, () -> "suggestExpressions, " + tokens.size() + " input tokens");
        List<PreFilterScore> scored = new ArrayList<>();
        for (RegisteredExpression re : reg.getExpressions()) {
            PreFilterScore pfs = PreFilter.run(tokens, re.pattern(), re, opts, debug);
            if (pfs != null) scored.add(pfs);
        }
        return analyze(scored, tokens, reg.getTypeRegistry(), env, opts, debug);
    }

    /**
     * Finds the closest matching condition patterns for unrecognized input tokens.
     */
    public static @NotNull List<Suggestion> suggestConditions(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env) {
        return suggestConditions(tokens, reg, env, SimulatorOptions.defaults());
    }

    /**
     * Finds the closest matching condition patterns with custom simulator options.
     */
    public static @NotNull List<Suggestion> suggestConditions(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts) {
        return suggestConditions(tokens, reg, env, opts, SimulatorDebug.OFF);
    }

    /**
     * Finds the closest matching condition patterns with custom options and debug capture.
     */
    public static @NotNull List<Suggestion> suggestConditions(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts, @NotNull SimulatorDebug debug) {
        if (tokens.isEmpty()) return List.of();
        debug.emit(Verbosity.RESULT, 0, () -> "suggestConditions, " + tokens.size() + " input tokens");
        List<PreFilterScore> scored = new ArrayList<>();
        for (RegisteredCondition rc : reg.getConditionRegistry().getConditions()) {
            PreFilterScore pfs = PreFilter.run(tokens, rc.pattern(), null, opts, debug);
            if (pfs != null) scored.add(pfs);
        }
        return analyze(scored, tokens, reg.getTypeRegistry(), env, opts, debug);
    }

    /**
     * Finds the closest matching block patterns for unrecognized input tokens.
     */
    public static @NotNull List<Suggestion> suggestBlocks(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env) {
        return suggestBlocks(tokens, reg, env, SimulatorOptions.defaults());
    }

    /**
     * Finds the closest matching block patterns with custom simulator options.
     */
    public static @NotNull List<Suggestion> suggestBlocks(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts) {
        return suggestBlocks(tokens, reg, env, opts, SimulatorDebug.OFF);
    }

    /**
     * Finds the closest matching block patterns with custom options and debug capture.
     */
    public static @NotNull List<Suggestion> suggestBlocks(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts, @NotNull SimulatorDebug debug) {
        if (tokens.isEmpty()) return List.of();
        debug.emit(Verbosity.RESULT, 0, () -> "suggestBlocks, " + tokens.size() + " input tokens");
        List<PreFilterScore> scored = new ArrayList<>();
        for (RegisteredBlock rb : reg.getBlocks()) {
            PreFilterScore pfs = PreFilter.run(tokens, rb.pattern(), null, opts, debug);
            if (pfs != null) scored.add(pfs);
        }
        return analyze(scored, tokens, reg.getTypeRegistry(), env, opts, debug);
    }

    /**
     * Finds the closest matching patterns across both statements and expressions.
     */
    public static @NotNull List<Suggestion> suggestStatementsAndExpressions(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env) {
        return suggestStatementsAndExpressions(tokens, reg, env, SimulatorOptions.defaults());
    }

    /**
     * Finds the closest matching statements/expressions with custom simulator options.
     */
    public static @NotNull List<Suggestion> suggestStatementsAndExpressions(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts) {
        return suggestStatementsAndExpressions(tokens, reg, env, opts, SimulatorDebug.OFF);
    }

    /**
     * Finds the closest matching statements/expressions with custom options and debug capture.
     */
    public static @NotNull List<Suggestion> suggestStatementsAndExpressions(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts, @NotNull SimulatorDebug debug) {
        if (tokens.isEmpty()) return List.of();
        debug.emit(Verbosity.RESULT, 0, () -> "suggestStatementsAndExpressions, " + tokens.size() + " input tokens");
        List<PreFilterScore> scored = new ArrayList<>();
        for (RegisteredPattern rp : reg.getStatements()) {
            PreFilterScore pfs = PreFilter.run(tokens, rp.pattern(), rp, opts, debug);
            if (pfs != null) scored.add(pfs);
        }
        for (RegisteredExpression re : reg.getExpressions()) {
            PreFilterScore pfs = PreFilter.run(tokens, re.pattern(), re, opts, debug);
            if (pfs != null) scored.add(pfs);
        }
        return analyze(scored, tokens, reg.getTypeRegistry(), env, opts, debug);
    }

    private static @NotNull List<Suggestion> analyze(@NotNull List<PreFilterScore> scored, @NotNull List<Token> tokens, @NotNull TypeRegistry types, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts, @NotNull SimulatorDebug debug) {
        long start = debug.enabled(Verbosity.TIMING) ? System.nanoTime() : 0L;
        scored.sort(Comparator.comparingDouble(PreFilterScore::confidence).reversed());
        int limit = Math.min(opts.intValue(SimulatorOption.MAX_CANDIDATES), scored.size());
        debug.emit(Verbosity.CANDIDATES, 0, () -> "analyze " + scored.size() + " pre-filtered candidates, taking top " + limit);
        Map<Pattern, Suggestion> best = new LinkedHashMap<>();
        for (int i = 0; i < limit; i++) {
            PreFilterScore cs = scored.get(i);
            debug.emit(Verbosity.SCORED, 1, () -> "analyse #" + cs.pattern().raw() + " (preFilter=" + String.format("%.3f", cs.confidence()) + ")");
            long candStart = debug.enabled(Verbosity.TIMING) ? System.nanoTime() : 0L;
            Suggestion s = TryMatch.run(tokens, cs, types, env, opts, debug);
            if (debug.enabled(Verbosity.TIMING)) {
                Trace.timing(debug, "tryMatch " + cs.pattern().raw(), System.nanoTime() - candStart);
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
        double minReport = opts.doubleValue(SimulatorOption.MIN_REPORT_CONFIDENCE);
        List<Suggestion> results = new ArrayList<>(best.size());
        for (Suggestion s : best.values()) {
            if (s.confidence() >= minReport) results.add(s);
        }
        results.sort(Comparator.comparingDouble(Suggestion::confidence).reversed());
        int max = Math.min(opts.intValue(SimulatorOption.MAX_SUGGESTIONS), results.size());
        List<Suggestion> ordered = List.copyOf(results.subList(0, max));
        debug.trace(new TraceEvent.Ranked(ordered));
        if (debug.enabled(Verbosity.RANKED)) {
            debug.emit(Verbosity.RANKED, 0, () -> "ranked " + ordered.size() + " suggestion(s)");
            for (int i = 0; i < ordered.size(); i++) {
                Suggestion s = ordered.get(i);
                int rank = i;
                debug.emit(Verbosity.RANKED, 1, () -> "#" + rank + " " + String.format("%.3f", s.confidence()) + "  " + s.pattern().raw());
            }
        }
        if (debug.enabled(Verbosity.TIMING)) {
            Trace.timing(debug, "analyze (total)", System.nanoTime() - start);
        }
        return ordered;
    }
}
