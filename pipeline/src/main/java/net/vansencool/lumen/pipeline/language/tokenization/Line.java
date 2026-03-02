package net.vansencool.lumen.pipeline.language.tokenization;

import java.util.List;

/**
 * A tokenized source line, combining indentation metadata with the resulting token list.
 *
 * <p>Blank lines and comment lines (starting with {@code #}) are excluded by the
 * {@link Tokenizer} and never appear as {@code Line} objects.
 *
 * @param indent     the indentation depth in spaces (tabs are counted as 4 spaces)
 * @param lineNumber the 1-based line number in the original source file
 * @param raw        the raw (non-indentation) content of the source line
 * @param tokens     the tokens produced from {@link #raw()}
 * @see Tokenizer
 */
public record Line(
        int indent,
        int lineNumber,
        String raw,
        List<Token> tokens
) {
}
