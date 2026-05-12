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
import dev.lumenlang.lumen.pipeline.language.simulator.result.Position;
import dev.lumenlang.lumen.pipeline.language.simulator.result.Suggestion;
import dev.lumenlang.lumen.pipeline.language.simulator.walk.TryMatch;
import dev.lumenlang.lumen.pipeline.language.simulator.walk.Walker;
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
     * Finds the closest matching statement patterns.
     */
    public static @NotNull List<Suggestion> suggestStatements(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env) {
        return suggestStatements(tokens, reg, env, SimulatorOptions.defaults());
    }

    /**
     * Finds the closest matching statement patterns with custom options.
     */
    public static @NotNull List<Suggestion> suggestStatements(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts) {
        return suggestStatements(tokens, reg, env, opts, SimulatorDebug.OFF);
    }

    /**
     * Finds the closest matching statement patterns with custom options and debug capture.
     */
    public static @NotNull List<Suggestion> suggestStatements(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts, @NotNull SimulatorDebug debug) {
        if (tokens.isEmpty()) return List.of();
        debug.emit(Verbosity.RESULT, 0, () -> "suggestStatements, " + tokens.size() + " input tokens");
        List<PreFilterScore> scored = new ArrayList<>();
        for (RegisteredPattern rp : reg.getStatements()) {
            PreFilterScore pfs = PreFilter.run(tokens, rp.pattern(), rp, opts, debug);
            if (pfs != null) scored.add(pfs);
        }
        return analyze(scored, tokens, reg.getTypeRegistry(), env, opts, debug);
    }

    /**
     * Walks every candidate pattern of {@code scope} directionally against {@code tokens} and
     * returns a {@link Position} per pre-filtered survivor.
     *
     * <p>Empty input returns every registered pattern of the scope with
     * {@code consumedTokens = 0} and {@code atPart} set to the first part.
     *
     * @param scope which pattern set to walk
     */
    public static @NotNull List<Position> positions(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull Scope scope) {
        return positions(tokens, reg, env, scope, SimulatorOptions.defaults());
    }

    /**
     * {@link #positions(List, PatternRegistry, TypeEnvImpl, Scope)} with custom options.
     */
    public static @NotNull List<Position> positions(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull Scope scope, @NotNull SimulatorOptions opts) {
        return positions(tokens, reg, env, scope, opts, SimulatorDebug.OFF);
    }

    /**
     * {@link #positions(List, PatternRegistry, TypeEnvImpl, Scope)} with custom options and
     * debug capture.
     */
    public static @NotNull List<Position> positions(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull Scope scope, @NotNull SimulatorOptions opts, @NotNull SimulatorDebug debug) {
        TypeRegistry types = reg.getTypeRegistry();
        List<Pattern> candidates = patternsFor(reg, scope);
        if (tokens.isEmpty()) {
            List<Position> out = new ArrayList<>(candidates.size());
            for (Pattern p : candidates) out.add(Walker.walk(tokens, p, 1.0, types, env));
            return out;
        }
        List<PreFilterScore> scored = new ArrayList<>();
        for (Pattern p : candidates) {
            PreFilterScore pfs = PreFilter.run(tokens, p, null, opts, debug);
            if (pfs != null) scored.add(pfs);
        }
        scored.sort(Comparator.comparingDouble(PreFilterScore::confidence).reversed());
        int limit = Math.min(opts.intValue(SimulatorOption.MAX_CANDIDATES), scored.size());
        List<Position> out = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            PreFilterScore cs = scored.get(i);
            out.add(Walker.walk(tokens, cs.pattern(), cs.confidence(), types, env));
        }
        return out;
    }

    public static @NotNull List<Pattern> patternsFor(@NotNull PatternRegistry reg, @NotNull Scope scope) {
        List<Pattern> out = new ArrayList<>();
        switch (scope) {
            case ROOT_LEVEL -> {
                for (RegisteredBlock rb : reg.getBlocks()) {
                    if (rb.supportsRootLevel()) out.add(rb.pattern());
                }
            }
            case INSIDE_BLOCK -> {
                for (RegisteredPattern rp : reg.getStatements()) out.add(rp.pattern());
                for (RegisteredBlock rb : reg.getBlocks()) {
                    if (rb.supportsBlock()) out.add(rb.pattern());
                }
            }
            case EXPRESSION -> {
                for (RegisteredExpression re : reg.getExpressions()) out.add(re.pattern());
            }
            case CONDITION -> {
                for (RegisteredCondition rc : reg.getConditionRegistry().getConditions()) out.add(rc.pattern());
            }
        }
        return out;
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

    /**
     * Which set of registered patterns the editor-facing entry walks.
     */
    public enum Scope {

        /**
         * Top-level: block patterns that allow root-level usage only.
         */
        ROOT_LEVEL,

        /**
         * Inside a block: statements plus block patterns that allow nesting.
         */
        INSIDE_BLOCK,

        /**
         * Expression patterns only.
         */
        EXPRESSION,

        /**
         * Condition patterns.
         */
        CONDITION
    }
}
