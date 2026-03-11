package net.vansencool.lumen.api.emit;

import org.jetbrains.annotations.NotNull;

/**
 * A single lexical token from a Lumen script line.
 *
 * <p>Tokens carry their text content, lexical type, and source position.
 * String tokens have their surrounding quotes already stripped.
 *
 * @see ScriptLine
 */
public interface ScriptToken {

    /**
     * Returns the text content of this token.
     *
     * <p>For string tokens, this is the content without surrounding quotes.
     *
     * @return the token text
     */
    @NotNull String text();

    /**
     * Returns the lexical type of this token.
     *
     * @return the token type
     */
    @NotNull TokenType tokenType();

    /**
     * Returns the 1-based source line number where this token appears.
     *
     * @return the line number
     */
    int line();

    /**
     * Returns the character offset of the first character within the source line.
     *
     * @return the start offset
     */
    int start();

    /**
     * Returns the character offset one past the last character within the source line.
     *
     * @return the end offset
     */
    int end();

    /**
     * Classifies the lexical category of a script token.
     */
    enum TokenType {
        /**
         * An identifier or keyword, including possessives like {@code player's}.
         */
        IDENT,
        /**
         * A numeric literal (integer or decimal).
         */
        NUMBER,
        /**
         * A double-quoted string literal with quotes stripped.
         */
        STRING,
        /**
         * A single non-alphanumeric, non-whitespace character.
         */
        SYMBOL
    }
}
