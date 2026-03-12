package dev.lumenlang.lumen.api.emit;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A single child line within a block, providing access to its tokens and source information.
 *
 * <p>Used by {@link BlockFormHandler} to iterate and process the children of a custom block.
 *
 * @see BlockFormHandler
 * @see ScriptToken
 */
public interface ScriptLine {

    /**
     * Returns the 1-based source line number.
     *
     * @return the line number
     */
    int lineNumber();

    /**
     * Returns the raw source text of this line (without indentation).
     *
     * @return the raw text
     */
    @NotNull String raw();

    /**
     * Returns the tokens that make up this line.
     *
     * @return an unmodifiable list of tokens
     */
    @NotNull List<? extends ScriptToken> tokens();
}
