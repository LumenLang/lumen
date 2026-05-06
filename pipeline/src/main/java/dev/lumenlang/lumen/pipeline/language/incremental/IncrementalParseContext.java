package dev.lumenlang.lumen.pipeline.language.incremental;

import org.jetbrains.annotations.NotNull;

/**
 * Thread-local state attaching a {@link MatchCache} to the active parse so that interior
 * call sites (e.g. inside the code emitter) can consult the cache without threading it
 * through every method signature.
 */
public final class IncrementalParseContext {

    private static final ThreadLocal<IncrementalParseContext> CURRENT = ThreadLocal.withInitial(() -> new IncrementalParseContext(MatchCache.NOOP));

    private final MatchCache cache;

    public IncrementalParseContext(@NotNull MatchCache cache) {
        this.cache = cache;
    }

    /**
     * Returns the context active on the current thread, or a no-op context when none was set.
     */
    public static @NotNull IncrementalParseContext current() {
        return CURRENT.get();
    }

    /**
     * Sets {@code ctx} as the active context for the current thread.
     */
    public static void set(@NotNull IncrementalParseContext ctx) {
        CURRENT.set(ctx);
    }

    /**
     * Resets the current thread's context to a no-op.
     */
    public static void clear() {
        CURRENT.remove();
    }

    /**
     * Returns the match cache associated with this context.
     */
    public @NotNull MatchCache cache() {
        return cache;
    }
}
