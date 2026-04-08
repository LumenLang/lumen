package dev.lumenlang.lumen.api.bus;

/**
 * A {@link LumenEvent} that can be cancelled by a subscriber.
 *
 * <p>When an event is cancelled, subsequent subscribers still receive it
 * (unless the event bus implementation skips them), and the poster can
 * check {@link #cancelled()} to decide whether to proceed.
 */
public abstract class Cancellable extends LumenEvent {

    private boolean cancelled;

    /**
     * Returns whether this event has been cancelled.
     *
     * @return true if cancelled
     */
    public boolean cancelled() {
        return cancelled;
    }

    /**
     * Sets whether this event is cancelled.
     *
     * @param cancelled true to cancel
     */
    public void cancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
