package dev.lumenlang.lumen.pipeline.language.suggestor.token;

import org.jetbrains.annotations.NotNull;

/**
 * The token-being-typed at the cursor.
 *
 * @param text       partial text of the token (empty when policy is COMPLETE)
 * @param policy     PREFIX when cursor sits inside a token, COMPLETE when right after whitespace
 *                   or at end of line
 * @param rangeStart source column where the active token starts (inclusive)
 * @param rangeEnd   source column where the active token ends (exclusive); equal to
 *                   {@code rangeStart} when policy is COMPLETE
 */
public record ActiveToken(@NotNull String text, @NotNull Policy policy, int rangeStart, int rangeEnd) {

    /**
     * Whether the cursor sits inside a token (PREFIX) or right after one (COMPLETE).
     */
    public enum Policy {

        /**
         * Cursor is inside a token. {@link ActiveToken#text} holds the partial text.
         */
        PREFIX,

        /**
         * Cursor is after whitespace or at end of line. {@link ActiveToken#text} is empty.
         */
        COMPLETE
    }
}
