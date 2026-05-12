package dev.lumenlang.lumen.pipeline.language.simulator.result;

import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;

/**
 * A specific issue detected in the input tokens relative to a candidate pattern.
 */
public sealed interface SuggestionIssue {

    /**
     * A token in the input that is a likely typo of a pattern literal.
     *
     * @param token    the input token containing the typo
     * @param expected the correct literal text
     */
    record Typo(@NotNull Token token, @NotNull String expected) implements SuggestionIssue {
        @Override
        public @NotNull String toString() {
            return "typo: '" + token.text() + "' (col " + token.start() + ") should be '" + expected + "'";
        }
    }

    /**
     * A token that was rejected by a type binding in the pattern.
     *
     * @param token     the rejected input token
     * @param bindingId the type binding id that rejected it
     * @param reason    a human readable rejection reason produced by the binding
     */
    record TypeMismatch(@NotNull Token token, @NotNull String bindingId,
                        @NotNull String reason) implements SuggestionIssue {
        @Override
        public @NotNull String toString() {
            return "type mismatch: '" + token.text() + "' (col " + token.start() + ") is not a " + bindingId + " (" + reason + ")";
        }
    }

    /**
     * A type binding that expects input but received none (missing tokens).
     *
     * @param bindingId the type binding ID that is missing
     * @param atColumn  the column where a caret should point (the gap where the binding's
     *                  value should have appeared), or {@code -1} when no precise column is
     *                  known
     */
    record MissingBinding(@NotNull String bindingId, int atColumn) implements SuggestionIssue {
        @Override
        public @NotNull String toString() {
            return atColumn < 0 ? "missing binding: " + bindingId : "missing binding: " + bindingId + " (col " + atColumn + ")";
        }
    }

    /**
     * The pattern's handler accepted the syntactic match but rejected it semantically by
     * throwing a {@link DiagnosticException}. The diagnostic title carries the underlying reason.
     *
     * @param title the diagnostic title from the thrown exception
     */
    record HandlerDiagnostic(@NotNull String title) implements SuggestionIssue {
        @Override
        public @NotNull String toString() {
            return "handler rejected: " + title;
        }
    }

    /**
     * Pattern expected a literal keyword that the input never produced.
     *
     * @param literal         the literal text the pattern expected
     * @param afterTokenIndex token index after which the literal was missing, or {@code -1}
     *                        when the literal was missing from the very start
     */
    record MissingLiteral(@NotNull String literal, int afterTokenIndex) implements SuggestionIssue {
        @Override
        public @NotNull String toString() {
            return "missing literal: '" + literal + "' (after token " + afterTokenIndex + ")";
        }
    }

    /**
     * Input ended while the pattern still expected more content.
     *
     * @param expectedNext short label naming what the pattern expected next (literal text or
     *                     binding id)
     */
    record IncompleteInput(@NotNull String expectedNext) implements SuggestionIssue {
        @Override
        public @NotNull String toString() {
            return "incomplete: expected '" + expectedNext + "' next";
        }
    }
}
