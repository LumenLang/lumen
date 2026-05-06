package dev.lumenlang.lumen.pipeline.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Outcome of a type annotation parse attempt: either a {@link ParsedType} on
 * success or a positioned error on failure.
 */
public final class ParseResult {

    private final boolean ok;
    private final @Nullable ParsedType parsed;
    private final int errorOffset;
    private final @Nullable String errorMessage;
    private final @Nullable String errorSuggestion;

    private ParseResult(boolean ok, @Nullable ParsedType parsed, int errorOffset, @Nullable String errorMessage, @Nullable String errorSuggestion) {
        this.ok = ok;
        this.parsed = parsed;
        this.errorOffset = errorOffset;
        this.errorMessage = errorMessage;
        this.errorSuggestion = errorSuggestion;
    }

    /**
     * Wraps a successful parse outcome.
     */
    public static @NotNull ParseResult success(@NotNull ParsedType parsed) {
        return new ParseResult(true, parsed, -1, null, null);
    }

    /**
     * Wraps a failed parse with the offending token offset, the error message, and an
     * optional fuzzy match suggestion.
     */
    public static @NotNull ParseResult failure(int errorOffset, @NotNull String errorMessage, @Nullable String errorSuggestion) {
        return new ParseResult(false, null, errorOffset, errorMessage, errorSuggestion);
    }

    /**
     * Whether the parse succeeded.
     */
    public boolean ok() {
        return ok;
    }

    /**
     * The resolved type when the parse succeeded, or {@code null} on failure.
     */
    public @Nullable ParsedType parsed() {
        return parsed;
    }

    /**
     * Token index where parsing failed, or {@code -1} on success.
     */
    public int errorOffset() {
        return errorOffset;
    }

    /**
     * Description of why parsing failed, or {@code null} on success.
     */
    public @Nullable String errorMessage() {
        return errorMessage;
    }

    /**
     * Closest known type name when the parser found a fuzzy match, or {@code null} otherwise.
     */
    public @Nullable String errorSuggestion() {
        return errorSuggestion;
    }
}
