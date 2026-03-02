package net.vansencool.lumen.pipeline.java.compiled;

import org.jetbrains.annotations.NotNull;

/**
 * Thrown to indicate that a Lumen script variable was null when a value was
 * expected.
 *
 * <p>
 * Unlike a raw {@link NullPointerException}, this exception carries the
 * script-level variable name so that error reports can show the exact variable
 * the author wrote rather than the JVM-generated parameter or field name.
 *
 * <p>
 * Generated code and addon utilities may throw this in place of a bare NPE
 * wherever the variable name is known at the throw site.
 */
@SuppressWarnings("unused")
public final class LumenNullException extends RuntimeException {

    private final @NotNull String scriptVarName;

    /**
     * Creates a new exception for the given script variable name.
     *
     * @param scriptVarName the name of the Lumen variable that was null
     */
    public LumenNullException(@NotNull String scriptVarName) {
        super("'" + scriptVarName + "' was null");
        this.scriptVarName = scriptVarName;
    }

    /**
     * Creates a new exception for the given script variable name with an
     * additional detail message.
     *
     * @param scriptVarName the name of the Lumen variable that was null
     * @param detail        optional detail appended in parentheses (e.g. the
     *                      method that was attempted)
     */
    public LumenNullException(@NotNull String scriptVarName, @NotNull String detail) {
        super("'" + scriptVarName + "' was null" + (detail.isEmpty() ? "" : " (" + detail + ")"));
        this.scriptVarName = scriptVarName;
    }

    /**
     * Returns the script-level variable name that was null.
     *
     * @return the Lumen variable name
     */
    public @NotNull String scriptVarName() {
        return scriptVarName;
    }
}
