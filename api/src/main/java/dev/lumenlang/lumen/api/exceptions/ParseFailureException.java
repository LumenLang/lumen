package dev.lumenlang.lumen.api.exceptions;

import org.jetbrains.annotations.NotNull;

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
 *
 * <p>Hard errors (malformed templates, internal logic failures, etc.) should still be thrown as
 * ordinary {@link RuntimeException}s and will not be silenced.
 */
public final class ParseFailureException extends RuntimeException {

    /**
     * Creates a new {@code ParseFailureException} with the supplied message.
     *
     * @param message a human-readable description of why parsing failed, used in debug output
     */
    public ParseFailureException(@NotNull String message) {
        super(message, null, true, false);
    }
}
