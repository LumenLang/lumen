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
     * @param event the event to post
     * @param <T>   the event type
     * @return the event after all subscribers have been invoked
     */
    <T extends LumenEvent> @NotNull T post(@NotNull T event);
}
