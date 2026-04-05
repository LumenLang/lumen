package dev.lumenlang.lumen.pipeline.language.tokenization;

import dev.lumenlang.lumen.api.emit.ScriptToken;
import org.jetbrains.annotations.NotNull;

/**
 * An atomic unit of text produced by the {@link Tokenizer}.
 *
 * <p>Each token represents a single lexical element: an identifier, a number, a string literal,
 * or a symbol. Tokens carry their source position ({@link #line()}, {@link #start()},
 * {@link #end()}) to support error reporting.
 *
 * <p>String tokens have their surrounding quotes stripped and escape sequences resolved; the
 * {@link #text()} field contains the <em>content</em> of the string, not the raw source.
 *
 * @param kind  the lexical category of this token
 * @param text  the text content (for strings: content without quotes; for others: raw text)
 * @param line  the 1-based source line number
 * @param start the character offset of the first character within the source line
 * @param end   the character offset one past the last character within the source line
 */
public record Token(
        TokenKind kind,
        String text,
        int line,
        int start,
        int end
) implements ScriptToken {

    private static final TokenType[] TYPE_MAP = {
            TokenType.IDENT, TokenType.NUMBER, TokenType.STRING, TokenType.SYMBOL
    };

    @Override
    public @NotNull TokenType tokenType() {
        return TYPE_MAP[kind.ordinal()];
    }
}
