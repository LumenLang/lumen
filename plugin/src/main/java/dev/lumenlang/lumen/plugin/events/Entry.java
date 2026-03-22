package dev.lumenlang.lumen.plugin.events;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;

/**
 * Represents a single event handler entry, associating a target object with a MethodHandle to invoke when the event is fired.
 */
public final class Entry {

    private volatile @Nullable Object target;
    private volatile @Nullable MethodHandle handle;

    public Entry(@NotNull Object target, @NotNull MethodHandle handle) {
        this.target = target;
        this.handle = handle;
    }

    public @Nullable Object target() {
        return target;
    }

    public @Nullable MethodHandle handle() {
        return handle;
    }

    public void clear() {
        this.target = null;
        this.handle = null;
    }
}
