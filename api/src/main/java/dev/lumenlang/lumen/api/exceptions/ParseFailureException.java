package dev.lumenlang.lumen.api.exceptions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Thrown by a type binding implementation to signal that the given tokens cannot be
 * parsed by this binding, and that pattern matching should move on to the next candidate pattern.
 *
 * <p>This is a <em>control-flow exception</em>, not an error. It distinguishes "I don't recognise
 * this input" from a genuine runtime failure.
 *
 * <p>Type bindings should throw this exception in their parse or consumeCount implementation
 * whenever the supplied token list cannot be resolved -- for example, when a {@code PLAYER}
 * binding encounters an identifier that is not a known player reference.
 */
public final class ParseFailureException extends RuntimeException {

    private final @Nullable Supplier<String> lazyMessage;
    private @Nullable String resolved;

    /**
     * Creates a new {@code ParseFailureException} with the supplied message.
     *
     * @param message a human-readable description of why parsing failed
     */
    public ParseFailureException(@NotNull String message) {
        super(message, null, true, false);
        this.lazyMessage = null;
        this.resolved = message;
    }

    /**
     * Creates a {@code ParseFailureException} whose message is computed only when first read.
     * Use this when constructing the message is expensive (e.g. fuzzy lookups against large
     * dictionaries) and may be discarded by the caller.
     *
     * @param messageSupplier supplier invoked once on first {@link #getMessage()} call
     */
    public ParseFailureException(@NotNull Supplier<String> messageSupplier) {
        super(null, null, true, false);
        this.lazyMessage = messageSupplier;
    }

    @Override
    public @Nullable String getMessage() {
        if (resolved != null) return resolved;
        if (lazyMessage != null) {
            resolved = lazyMessage.get();
            return resolved;
        }
        return null;
    }
}
