package dev.lumenlang.lumen.pipeline.language.simulator.debug;

import dev.lumenlang.lumen.pipeline.language.simulator.debug.trace.SimulatorTracer;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.trace.TraceEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Debug configuration for the pattern simulator: verbosity for human-readable line emission and
 * a tracer for structured event capture. Pass {@link #OFF} to disable all debug work.
 *
 * @param verbosity controls which sink lines are written
 * @param sink      receives formatted lines when verbosity allows
 * @param tracer    receives every emitted {@link TraceEvent} regardless of verbosity
 */
public record SimulatorDebug(@NotNull Verbosity verbosity, @NotNull DebugSink sink,
                             @NotNull SimulatorTracer tracer) {

    /**
     * No verbosity, no-op sink, no-op tracer.
     */
    public static final SimulatorDebug OFF = new SimulatorDebug(Verbosity.OFF, DebugSink.NOOP, SimulatorTracer.NOOP);

    /**
     * {@code true} when the configured verbosity is at least {@code level}.
     */
    public boolean enabled(@NotNull Verbosity level) {
        return verbosity.atLeast(level);
    }

    /**
     * Writes a sink line at {@code depth} when {@code level} is enabled. The supplier is invoked
     * only when the level is active, so callers may build expensive strings inline.
     */
    public void emit(@NotNull Verbosity level, int depth, @NotNull Supplier<String> line) {
        if (verbosity.atLeast(level)) sink.write(depth, line.get());
    }

    /**
     * Routes a structured event to the tracer.
     */
    public void trace(@NotNull TraceEvent event) {
        tracer.onEvent(event);
    }

    /**
     * Builds an instance that writes to {@link DebugSink#STDERR} at the given verbosity, with a
     * no-op tracer.
     */
    public static @NotNull SimulatorDebug stderr(@NotNull Verbosity verbosity) {
        return new SimulatorDebug(verbosity, DebugSink.STDERR, SimulatorTracer.NOOP);
    }
}
