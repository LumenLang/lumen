package net.vansencool.lumen.plugin.events;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;

/**
 * Represents a single event handler entry, associating a target object with a MethodHandle to invoke when the event is fired.
 *
 * @param target the instance on which the event handler method should be invoked
 * @param handle a MethodHandle representing the event handler method to call
 */
public record Entry(@NotNull Object target, @NotNull MethodHandle handle) {
}
