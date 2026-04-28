package dev.lumenlang.lumen.pipeline.language.exceptions;

import dev.lumenlang.lumen.pipeline.language.resolve.PatternSimulator;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A {@link RuntimeException} subclass that carries token position information.
 */
public class TokenCarryingException extends RuntimeException {

    private final List<Token> tokens;
    private final List<PatternSimulator.Suggestion> suggestions;

    /**
     * Creates a new TokenCarryingException with the given message and associated tokens.
     *
     * @param message the error message
     * @param tokens  the list of tokens associated with the error, used for position tracking
     */
    public TokenCarryingException(@NotNull String message, @NotNull List<Token> tokens) {
        this(message, tokens, List.of());
    }

    /**
     * Creates a new TokenCarryingException with the given message, associated tokens, and suggestions.
     *
     * @param message     the error message
     * @param tokens      the list of tokens associated with the error, used for position tracking
     * @param suggestions simulation suggestions from the pattern simulator
     */
    public TokenCarryingException(@NotNull String message, @NotNull List<Token> tokens, @NotNull List<PatternSimulator.Suggestion> suggestions) {
        super(message);
        this.tokens = List.copyOf(tokens);
        this.suggestions = List.copyOf(suggestions);
    }

    public @NotNull List<Token> tokens() {
        return tokens;
    }

    public @NotNull List<PatternSimulator.Suggestion> suggestions() {
        return suggestions;
    }
}
