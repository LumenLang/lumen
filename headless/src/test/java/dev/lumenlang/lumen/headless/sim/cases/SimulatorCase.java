package dev.lumenlang.lumen.headless.sim.cases;

import dev.lumenlang.lumen.headless.sim.debug.TextDebugSink;
import dev.lumenlang.lumen.headless.sim.snapshot.Snapshot;
import dev.lumenlang.lumen.headless.sim.snapshot.SuggestionSnap;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator.Suggestion;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator.SuggestionIssue;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.SimulatorDebug;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.Verbosity;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.trace.SimulatorTracer;
import dev.lumenlang.lumen.pipeline.language.simulator.options.SimulatorOptions;
import dev.lumenlang.lumen.pipeline.language.tokenization.Line;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import dev.lumenlang.lumen.pipeline.language.tokenization.Tokenizer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent test case for the pattern simulator. Each case describes the input and environment, runs
 * the simulator on demand, and produces a {@link Snapshot} which the harness compares against a
 * stored baseline.
 */
public final class SimulatorCase {

    private final SimulatorRunner runner;
    private final String input;
    private @NotNull String name;
    private @NotNull EnvSimulator env = EnvSimulator.empty();
    private @NotNull SimulatorOptions options = SimulatorOptions.defaults();
    private @Nullable Verbosity debugLevel;

    private SimulatorCase(@NotNull SimulatorRunner runner, @NotNull String input) {
        this.runner = runner;
        this.input = input;
        this.name = runner.name().toLowerCase() + ": " + input;
    }

    /**
     * Builds a case running against statement and expression patterns.
     */
    public static @NotNull SimulatorCase statement(@NotNull String input) {
        return new SimulatorCase(SimulatorRunner.STATEMENT, input);
    }

    /**
     * Builds a case running against expression patterns only.
     */
    public static @NotNull SimulatorCase expression(@NotNull String input) {
        return new SimulatorCase(SimulatorRunner.EXPRESSION, input);
    }

    /**
     * Builds a case running against condition patterns.
     */
    public static @NotNull SimulatorCase condition(@NotNull String input) {
        return new SimulatorCase(SimulatorRunner.CONDITION, input);
    }

    /**
     * Builds a case running against block patterns.
     */
    public static @NotNull SimulatorCase block(@NotNull String input) {
        return new SimulatorCase(SimulatorRunner.BLOCK, input);
    }

    private static @NotNull List<Token> tokenize(@NotNull String input) {
        List<Line> lines = new Tokenizer().tokenize(input);
        if (lines.isEmpty()) return List.of();
        return lines.get(0).tokens();
    }

    private static @NotNull List<SuggestionSnap> serializeSuggestions(@NotNull List<Suggestion> suggestions) {
        List<SuggestionSnap> out = new ArrayList<>(suggestions.size());
        for (Suggestion s : suggestions) {
            List<String> issues = new ArrayList<>(s.issues().size());
            for (SuggestionIssue issue : s.issues()) issues.add(issue.toString());
            out.add(new SuggestionSnap(s.pattern().raw(), round3(s.confidence()), issues));
        }
        return out;
    }

    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    /**
     * Overrides the case display name shown in test reports and snapshot headers.
     */
    public @NotNull SimulatorCase named(@NotNull String name) {
        this.name = name;
        return this;
    }

    /**
     * Configures the env (variables, globals, data schemas) visible to the simulator.
     */
    public @NotNull SimulatorCase env(@NotNull EnvSimulator env) {
        this.env = env;
        return this;
    }

    /**
     * Overrides the {@link SimulatorOptions} used by this case.
     */
    public @NotNull SimulatorCase options(@NotNull SimulatorOptions options) {
        this.options = options;
        return this;
    }

    /**
     * Enables per-case debug output at the given verbosity, routed through
     * {@link TextDebugSink#stdout()}.
     */
    public @NotNull SimulatorCase withDebug(@NotNull Verbosity level) {
        this.debugLevel = level;
        return this;
    }

    /**
     * Runs the simulator and produces a {@link Snapshot} of the result.
     */
    public @NotNull Snapshot run() {
        List<Token> tokens = tokenize(input);
        TypeEnvImpl typeEnv = env.build();
        PatternRegistry registry = PatternRegistry.instance();
        SimulatorDebug debug = resolveDebug();
        if (debug != SimulatorDebug.OFF) {
            System.out.println();
            System.out.println("=== sim debug [" + debug.verbosity() + "] " + name + " ===");
        }
        List<Suggestion> suggestions = runner.run(tokens, registry, typeEnv, options, debug);
        return new Snapshot(name, input, runner.name(), env.summary(), serializeSuggestions(suggestions));
    }

    /**
     * Display name used in test output and snapshot headers.
     */
    public @NotNull String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    private @NotNull SimulatorDebug resolveDebug() {
        if (debugLevel != null && debugLevel != Verbosity.OFF) {
            return new SimulatorDebug(debugLevel, TextDebugSink.stdout(), SimulatorTracer.NOOP);
        }
        String prop = System.getProperty("sim.debug");
        if (prop == null || prop.isBlank()) return SimulatorDebug.OFF;
        try {
            Verbosity v = Verbosity.valueOf(prop.trim().toUpperCase());
            return v == Verbosity.OFF ? SimulatorDebug.OFF : new SimulatorDebug(v, TextDebugSink.stdout(), SimulatorTracer.NOOP);
        } catch (IllegalArgumentException ignored) {
            return SimulatorDebug.OFF;
        }
    }
}
