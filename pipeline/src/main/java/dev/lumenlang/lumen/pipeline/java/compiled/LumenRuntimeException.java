package dev.lumenlang.lumen.pipeline.java.compiled;

import org.jetbrains.annotations.NotNull;

/**
 * Thrown at runtime when a null-check in generated script code fails.
 *
 * <p>This provides the script line number and original source text so that
 * error messages can point the user to the correct location in their
 * {@code .luma} file rather than a meaningless Java line number.
 */
@SuppressWarnings("unused")
public class LumenRuntimeException extends RuntimeException {

    private final int scriptLine;
    private final String scriptSource;

    /**
     * Creates a new runtime exception tied to a specific script line.
     *
     * @param message      the human-readable error message
     * @param scriptLine   the 1-based line number in the original script
     * @param scriptSource the raw source text of the script line
     */
    public LumenRuntimeException(@NotNull String message, int scriptLine, @NotNull String scriptSource) {
        super(message);
        this.scriptLine = scriptLine;
        this.scriptSource = scriptSource;
    }

    /**
     * Returns the 1-based line number in the original script.
     *
     * @return the script line number
     */
    public int scriptLine() {
        return scriptLine;
    }

    /**
     * Returns the raw source text of the script line that caused the error.
     *
     * @return the script source text
     */
    public @NotNull String scriptSource() {
        return scriptSource;
    }
}
