package dev.lumenlang.lumen.pipeline.language.exceptions;

import dev.lumenlang.lumen.pipeline.language.LumenCore;
import dev.lumenlang.lumen.pipeline.language.tokenization.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Thrown when a Lumen script contains an error that is detected during parsing or code generation.
 *
 * <p>This exception carries the source line number and (when available) the raw source text so
 * that error messages include precise location information, similar to Java compiler errors:
 *
 * <pre>
 * Script error on line 5: Variable 'whatever' does not exist
 *   5 | if whatever:
 *        ~~~~~~~~
 * </pre>
 *
 * <p>When token position information is available, a squiggly underline is shown under the
 * offending tokens to make the error location visually clear.
 *
 * <p>Handlers should throw a plain {@link RuntimeException} with a descriptive message; the
 * code-generation loop in {@link LumenCore} will catch it and wrap it in a
 * {@code LumenScriptException} that includes line context automatically.
 *
 * @see LumenCore
 */
@SuppressWarnings("unused")
public final class LumenScriptException extends RuntimeException {

    private final int line;
    private final @Nullable String rawLine;

    /**
     * Creates a new script exception.
     *
     * @param line    the 1-based source line number where the error occurred
     * @param rawLine the original source text of that line (may be {@code null})
     * @param detail  a human-readable description of the error
     */
    public LumenScriptException(int line, @Nullable String rawLine, @NotNull String detail) {
        super(formatMessage(line, rawLine, detail, -1, -1));
        this.line = line;
        this.rawLine = rawLine;
    }

    /**
     * Creates a new script exception that wraps an underlying cause.
     *
     * @param line    the 1-based source line number where the error occurred
     * @param rawLine the original source text of that line (may be {@code null})
     * @param detail  a human-readable description of the error
     * @param cause   the underlying exception
     */
    public LumenScriptException(int line, @Nullable String rawLine, @NotNull String detail, @NotNull Throwable cause) {
        super(formatMessage(line, rawLine, detail, -1, -1), cause);
        this.line = line;
        this.rawLine = rawLine;
    }

    /**
     * Creates a new script exception with column range for squiggly underline display.
     *
     * @param line     the 1-based source line number where the error occurred
     * @param rawLine  the original source text of that line (may be {@code null})
     * @param detail   a human-readable description of the error
     * @param colStart the 0-based starting column of the error
     * @param colEnd   the 0-based ending column (exclusive) of the error
     */
    public LumenScriptException(int line, @Nullable String rawLine, @NotNull String detail,
                                int colStart, int colEnd) {
        super(formatMessage(line, rawLine, detail, colStart, colEnd));
        this.line = line;
        this.rawLine = rawLine;
    }

    /**
     * Creates a new script exception with token-based position highlighting.
     *
     * <p>The squiggly underline spans from the start of the first token to the end of the last token.
     *
     * @param line    the 1-based source line number where the error occurred
     * @param rawLine the original source text of that line (may be {@code null})
     * @param detail  a human-readable description of the error
     * @param tokens  the tokens to underline
     */
    public LumenScriptException(int line, @Nullable String rawLine, @NotNull String detail,
                                @NotNull List<Token> tokens) {
        super(formatMessage(line, rawLine, detail,
                tokens.isEmpty() ? -1 : tokens.get(0).start(),
                tokens.isEmpty() ? -1 : tokens.get(tokens.size() - 1).end()));
        this.line = line;
        this.rawLine = rawLine;
    }

    private static @NotNull String formatMessage(int line, @Nullable String rawLine,
                                                 @NotNull String detail, int colStart, int colEnd) {
        StringBuilder sb = new StringBuilder();
        sb.append("Script error on line ").append(line).append(": ").append(detail);
        if (rawLine != null) {
            String prefix = "  " + line + " | ";
            sb.append('\n');
            sb.append(prefix).append(rawLine);
            if (colStart >= 0 && colEnd > colStart) {
                sb.append('\n');
                sb.append(" ".repeat(prefix.length() + colStart));
                sb.append("~".repeat(colEnd - colStart));
            }
        }
        return sb.toString();
    }

    /**
     * Returns the 1-based source line number where the error occurred.
     *
     * @return the line number
     */
    public int line() {
        return line;
    }

    /**
     * Returns the original raw source text of the offending line, or {@code null} if unavailable.
     *
     * @return the raw source line
     */
    public @Nullable String rawLine() {
        return rawLine;
    }
}
