package dev.lumenlang.lumen.headless.sim.cases;

import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator;
import dev.lumenlang.lumen.pipeline.language.simulator.PatternSimulator.Suggestion;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.SimulatorDebug;
import dev.lumenlang.lumen.pipeline.language.simulator.options.SimulatorOptions;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Selects which {@link PatternSimulator} entry point to invoke.
 */
public enum SimulatorRunner {

    /**
     * Runs the simulator against the registered statement patterns.
     */
    STATEMENT {
        @Override
        public @NotNull List<Suggestion> run(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts, @NotNull SimulatorDebug debug) {
            return PatternSimulator.suggestStatementsAndExpressions(tokens, reg, env, opts, debug);
        }
    },

    /**
     * Runs the simulator against the registered expression patterns.
     */
    EXPRESSION {
        @Override
        public @NotNull List<Suggestion> run(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts, @NotNull SimulatorDebug debug) {
            return PatternSimulator.suggestExpressions(tokens, reg, env, opts, debug);
        }
    },

    /**
     * Runs the simulator against the registered condition patterns.
     */
    CONDITION {
        @Override
        public @NotNull List<Suggestion> run(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts, @NotNull SimulatorDebug debug) {
            return PatternSimulator.suggestConditions(tokens, reg, env, opts, debug);
        }
    },

    /**
     * Runs the simulator against the registered block patterns.
     */
    BLOCK {
        @Override
        public @NotNull List<Suggestion> run(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts, @NotNull SimulatorDebug debug) {
            return PatternSimulator.suggestBlocks(tokens, reg, env, opts, debug);
        }
    };

    /**
     * Invokes the configured simulator entry point with the given debug bag.
     */
    public abstract @NotNull List<Suggestion> run(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts, @NotNull SimulatorDebug debug);

    /**
     * Convenience overload using {@link SimulatorDebug#OFF}.
     */
    public @NotNull List<Suggestion> run(@NotNull List<Token> tokens, @NotNull PatternRegistry reg, @NotNull TypeEnvImpl env, @NotNull SimulatorOptions opts) {
        return run(tokens, reg, env, opts, SimulatorDebug.OFF);
    }
}
