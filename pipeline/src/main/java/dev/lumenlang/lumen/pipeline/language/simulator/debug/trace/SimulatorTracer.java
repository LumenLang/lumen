package dev.lumenlang.lumen.pipeline.language.simulator.debug.trace;

import org.jetbrains.annotations.NotNull;

/**
 * Receives structured {@link TraceEvent} instances from the pattern simulator.
 */
@FunctionalInterface
public interface SimulatorTracer {

    /**
     * Discards every event.
     */
    SimulatorTracer NOOP = event -> {
    };

    /**
     * Called once per emitted event.
     */
    void onEvent(@NotNull TraceEvent event);
}
