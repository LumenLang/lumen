package dev.lumenlang.lumen.headless.sim.cases;

import dev.lumenlang.lumen.headless.sim.failure.AnyIssueMismatch;
import dev.lumenlang.lumen.headless.sim.failure.ConfidenceMismatch;
import dev.lumenlang.lumen.headless.sim.failure.ContainsPatternMismatch;
import dev.lumenlang.lumen.headless.sim.failure.CustomMismatch;
import dev.lumenlang.lumen.headless.sim.failure.Mismatch;
import dev.lumenlang.lumen.headless.sim.failure.PrimaryIssueMismatch;
import dev.lumenlang.lumen.headless.sim.failure.SuggestionCountMismatch;
import dev.lumenlang.lumen.headless.sim.failure.SuggestionPresenceMismatch;
import dev.lumenlang.lumen.headless.sim.debug.TextDebugSink;
import dev.lumenlang.lumen.headless.sim.failure.TopPatternMismatch;
import dev.lumenlang.lumen.headless.sim.report.SimulatorReport;
import dev.lumenlang.lumen.headless.sim.result.RunInsights;
import dev.lumenlang.lumen.headless.sim.result.SimulatorRunRecord;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator;
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
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Fluent test case for {@link PatternSimulator}. Each declared expectation produces a typed
 * {@link Mismatch} when it disagrees with the observed simulator output, and the resulting
 * {@link SimulatorRunRecord} is forwarded to {@link SimulatorReport} for unified rendering.
 */
public final class SimulatorCase {

    private final SimulatorRunner runner;
    private final String input;
    private final List<Check> checks = new ArrayList<>();
    private final EnumSet<Coverage> coverage = EnumSet.noneOf(Coverage.class);
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
     * Builds a case that runs the simulator against statement and expression patterns.
     */
    public static @NotNull SimulatorCase statement(@NotNull String input) {
        return new SimulatorCase(SimulatorRunner.STATEMENT, input);
    }

    /**
     * Builds a case that runs the simulator against expression patterns only.
     */
    public static @NotNull SimulatorCase expression(@NotNull String input) {
        return new SimulatorCase(SimulatorRunner.EXPRESSION, input);
    }

    /**
     * Builds a case that runs the simulator against condition patterns.
     */
    public static @NotNull SimulatorCase condition(@NotNull String input) {
        return new SimulatorCase(SimulatorRunner.CONDITION, input);
    }

    /**
     * Builds a case that runs the simulator against block patterns.
     */
    public static @NotNull SimulatorCase block(@NotNull String input) {
        return new SimulatorCase(SimulatorRunner.BLOCK, input);
    }

    private static @NotNull List<Token> tokenize(@NotNull String input) {
        List<Line> lines = new Tokenizer().tokenize(input);
        if (lines.isEmpty()) return List.of();
        return lines.get(0).tokens();
    }

    private static @NotNull List<String> issueClassNames(@NotNull List<SuggestionIssue> issues) {
        List<String> out = new ArrayList<>(issues.size());
        for (SuggestionIssue issue : issues) out.add(issue.getClass().getSimpleName());
        return out;
    }

    /**
     * Overrides the human-readable label rendered in test output.
     */
    public @NotNull SimulatorCase named(@NotNull String name) {
        this.name = name;
        return this;
    }

    /**
     * Configures the variables, globals, and data schemas visible to the simulator.
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
     * Enables per-case debug output at the given verbosity. Lines are routed through
     * {@link TextDebugSink#stdout()}. Pass {@link Verbosity#OFF} to disable explicitly.
     */
    public @NotNull SimulatorCase withDebug(@NotNull Verbosity level) {
        this.debugLevel = level;
        return this;
    }

    /**
     * Adds an expectation that the suggestion list is non-empty.
     */
    public @NotNull SimulatorCase expectAnySuggestions() {
        return addCheck(suggestions -> suggestions.isEmpty() ? new SuggestionPresenceMismatch(true, 0) : null);
    }

    /**
     * Adds an expectation that the suggestion list is empty.
     */
    public @NotNull SimulatorCase expectNoSuggestions() {
        return addCheck(suggestions -> suggestions.isEmpty() ? null : new SuggestionPresenceMismatch(false, suggestions.size()));
    }

    /**
     * Adds an expectation that the highest-confidence suggestion's pattern equals {@code patternRaw}.
     */
    public @NotNull SimulatorCase expectTopPattern(@NotNull String patternRaw) {
        coverage.add(Coverage.TOP_PATTERN);
        return addCheck(suggestions -> {
            String actual = suggestions.isEmpty() ? null : suggestions.get(0).pattern().raw();
            if (patternRaw.equals(actual)) return null;
            return new TopPatternMismatch(patternRaw, actual);
        });
    }

    /**
     * Adds an expectation that some suggestion in the returned list matches {@code patternRaw}.
     */
    public @NotNull SimulatorCase expectContainsPattern(@NotNull String patternRaw) {
        return addCheck(suggestions -> {
            for (Suggestion s : suggestions) {
                if (s.pattern().raw().equals(patternRaw)) return null;
            }
            List<String> actual = suggestions.stream().map(s -> s.pattern().raw()).toList();
            return new ContainsPatternMismatch(patternRaw, actual);
        });
    }

    /**
     * Adds an expectation that the top suggestion's primary issue is an instance of {@code issueType}.
     */
    public @NotNull SimulatorCase expectPrimaryIssue(@NotNull Class<? extends SuggestionIssue> issueType) {
        return expectPrimaryIssueInternal(issueType, null);
    }

    /**
     * Adds an expectation that the top suggestion's primary issue matches {@code issueType} and the supplied check.
     */
    public <I extends SuggestionIssue> @NotNull SimulatorCase expectPrimaryIssue(@NotNull Class<I> issueType, @NotNull Consumer<I> valueCheck) {
        return expectPrimaryIssueInternal(issueType, valueCheck);
    }

    /**
     * Adds an expectation that the top suggestion's issue list contains an instance of {@code issueType}.
     */
    public @NotNull SimulatorCase expectAnyIssue(@NotNull Class<? extends SuggestionIssue> issueType) {
        coverage.add(Coverage.ANY_ISSUE);
        return addCheck(suggestions -> {
            if (suggestions.isEmpty()) return new AnyIssueMismatch(issueType.getSimpleName(), List.of());
            List<SuggestionIssue> issues = suggestions.get(0).issues();
            for (SuggestionIssue issue : issues) {
                if (issueType.isInstance(issue)) return null;
            }
            return new AnyIssueMismatch(issueType.getSimpleName(), issueClassNames(issues));
        });
    }

    /**
     * Adds an expectation that the top suggestion's confidence is at least {@code minConfidence}.
     */
    public @NotNull SimulatorCase expectConfidenceAtLeast(double minConfidence) {
        coverage.add(Coverage.CONFIDENCE_MIN);
        return addCheck(suggestions -> {
            if (suggestions.isEmpty()) return new ConfidenceMismatch(minConfidence, null);
            double actual = suggestions.get(0).confidence();
            return actual >= minConfidence ? null : new ConfidenceMismatch(minConfidence, actual);
        });
    }

    /**
     * Adds an expectation that the suggestion list size is within the closed interval {@code [min, max]}.
     */
    public @NotNull SimulatorCase expectSuggestionCount(int min, int max) {
        coverage.add(Coverage.SUGGESTION_COUNT);
        return addCheck(suggestions -> {
            int n = suggestions.size();
            return n >= min && n <= max ? null : new SuggestionCountMismatch(min, max, n);
        });
    }

    /**
     * Escape hatch for assertions not covered by the built-in expectation methods. The supplied function
     * returns {@code null} when the check passes, otherwise a free-form reason string.
     *
     * @param description short label for the expectation
     * @param check       function returning a reason on failure or {@code null} on success
     */
    public @NotNull SimulatorCase expect(@NotNull String description, @NotNull Function<List<Suggestion>, String> check) {
        return addCheck(suggestions -> {
            String reason = check.apply(suggestions);
            return reason == null ? null : new CustomMismatch(description, reason);
        });
    }

    /**
     * Tokenizes the input, runs the simulator, evaluates every expectation, captures the resulting
     * {@link SimulatorRunRecord}, and throws an {@link AssertionError} when at least one mismatch is produced.
     */
    public void execute() {
        List<Token> tokens = tokenize(input);
        TypeEnvImpl typeEnv = env.build();
        PatternRegistry registry = PatternRegistry.instance();
        SimulatorDebug debug = resolveDebug();
        if (debug != SimulatorDebug.OFF) {
            System.out.println();
            System.out.println("=== sim debug [" + debug.verbosity() + "] " + name + " ===");
        }
        List<Suggestion> suggestions = runner.run(tokens, registry, typeEnv, options, debug);

        List<String> uncoveredNames = EnumSet.complementOf(coverage).stream().map(Enum::name).toList();
        List<String> recs = RunInsights.recommendations(suggestions, new LinkedHashSet<>(uncoveredNames));

        boolean observing = Boolean.getBoolean("sim.observe");
        List<Mismatch> mismatches = new ArrayList<>();
        if (!observing) {
            for (Check check : checks) {
                Mismatch m = check.evaluate(suggestions);
                if (m != null) mismatches.add(m);
            }
        }

        SimulatorRunRecord record = new SimulatorRunRecord(name, input, suggestions, uncoveredNames, recs, mismatches);
        SimulatorReport.record(record);
        if (!record.passed()) {
            throw new AssertionError(name + " failed with " + mismatches.size() + " mismatch(es)");
        }
    }

    /**
     * Display name used in test output.
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

    private @NotNull SimulatorCase addCheck(@NotNull Check check) {
        checks.add(check);
        return this;
    }

    private <I extends SuggestionIssue> @NotNull SimulatorCase expectPrimaryIssueInternal(@NotNull Class<I> issueType, @Nullable Consumer<I> valueCheck) {
        coverage.add(Coverage.PRIMARY_ISSUE);
        return addCheck(suggestions -> {
            if (suggestions.isEmpty()) return new PrimaryIssueMismatch(issueType.getSimpleName(), List.of(), null);
            List<SuggestionIssue> issues = suggestions.get(0).issues();
            if (issues.isEmpty()) return new PrimaryIssueMismatch(issueType.getSimpleName(), List.of(), null);
            SuggestionIssue primary = issues.get(0);
            List<String> actualTypes = issueClassNames(issues);
            if (!issueType.isInstance(primary)) {
                return new PrimaryIssueMismatch(issueType.getSimpleName(), actualTypes, null);
            }
            if (valueCheck == null) return null;
            try {
                valueCheck.accept(issueType.cast(primary));
                return null;
            } catch (AssertionError e) {
                return new PrimaryIssueMismatch(issueType.getSimpleName(), actualTypes, e.getMessage());
            }
        });
    }

    /**
     * Categories of expectation a case can declare. Used to compute recommendations
     * for any category the case did not cover.
     */
    private enum Coverage {
        TOP_PATTERN,
        PRIMARY_ISSUE,
        ANY_ISSUE,
        CONFIDENCE_MIN,
        SUGGESTION_COUNT
    }

    @FunctionalInterface
    private interface Check {
        @Nullable Mismatch evaluate(@NotNull List<Suggestion> suggestions);
    }
}
