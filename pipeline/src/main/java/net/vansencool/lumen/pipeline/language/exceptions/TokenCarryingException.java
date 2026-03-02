package net.vansencool.lumen.pipeline.language.exceptions;

import net.vansencool.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A {@link RuntimeException} subclass that carries token position information,
 * allowing callers to generate precise squiggly-underline error indicators.
 */
public class TokenCarryingException extends RuntimeException {

    private final List<Token> tokens;

    /**
     * Creates a new TokenCarryingException with the given message and associated tokens.
     *
     * @param message the error message
     * @param tokens  the list of tokens associated with the error, used for position tracking
     */
    public TokenCarryingException(@NotNull String message, @NotNull List<Token> tokens) {
        super(message);
        this.tokens = List.copyOf(tokens);
    }

    public @NotNull List<Token> tokens() {
        return tokens;
    }
}
