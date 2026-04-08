package dev.lumenlang.lumen.api.bus;

import org.jetbrains.annotations.NotNull;

/**
 * A lightweight publish/subscribe event bus for Lumen's internal events.
 *
 * <pre>{@code
 * EventBus bus = LumenProvider.bus();
 * bus.register(myListener);
 * bus.post(new ScriptLoadEvent(script));
 * }</pre>
 *
 * @see Subscribe
 * @see Async
 * @see Priority
 */
public interface EventBus {

    /**
     * Registers all {@link Subscribe} methods found on the given listener.
     *
     * @param listener the object whose annotated methods should be subscribed
     */
    void register(@NotNull Object listener);

    /**
     * Unregisters all subscriptions that belong to the given listener.
     *
     * @param listener the listener to remove
     */
    void unregister(@NotNull Object listener);

    /**
     * Posts an event synchronously on the calling thread.
     *
     * <p>Subscribers marked {@link Async} still run on a background thread.
     *
     * @param event the event to post
     * @param <T>   the event type
     * @return the event after all subscribers have been invoked
     */
    <T extends LumenEvent> @NotNull T post(@NotNull T event);

    /**
     * Posts an event asynchronously. All subscribers (including those not
     * annotated with {@link Async}) are invoked on a background thread.
     *
     * @param event the event to post
     * @param <T>   the event type
     */
    <T extends LumenEvent> void postAsync(@NotNull T event);
}
