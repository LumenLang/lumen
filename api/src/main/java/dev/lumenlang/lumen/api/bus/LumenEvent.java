package dev.lumenlang.lumen.api.bus;

/**
 * Base class for all events posted on the Lumen {@link EventBus}.
 *
 * <p>Events that should be cancellable must extend {@link Cancellable} instead.
 */
public abstract class LumenEvent {
}
