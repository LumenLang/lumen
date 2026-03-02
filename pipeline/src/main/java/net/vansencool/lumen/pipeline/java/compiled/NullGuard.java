package net.vansencool.lumen.pipeline.java.compiled;

import net.vansencool.lumen.api.codegen.EnvironmentAccess;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runtime null-safety utility used in generated script code.
 *
 * <p>During code generation, nullable expressions can be wrapped with
 * {@link #of(Object, String, int, String)} to produce a clear error message
 * referencing the original script source when a value is unexpectedly null.
 *
 * <p>Unlike a raw {@link NullPointerException} which shows a Java line number
 * from the generated class, a {@code NullGuard} failure throws a
 * {@link LumenRuntimeException} that carries the script line number and the
 * original script text so the user can locate the problem in their {@code .luma}
 * file instead of the generated Java.
 *
 * <h2>Generated Code Example</h2>
 * <pre>{@code
 * // script line 5: send target "Hello"
 * NullGuard.of(target, "target", 5, "send target \"Hello\"").sendMessage("Hello");
 * }</pre>
 */
@SuppressWarnings("unused") // It is used in generated code
public final class NullGuard {

    private NullGuard() {
    }

    /**
     * Generates a {@code NullGuard.of(expr, "description")} Java source code fragment.
     *
     * <p>The expression text is properly escaped before embedding in the description
     * string literal, so generated Java expressions containing quotes or backslashes
     * will not break the output.
     *
     * @param javaExpr the Java expression source code to null-guard
     * @return the {@code NullGuard.of(...)} call as Java source text
     */
    public static @NotNull String codegen(@NotNull String javaExpr) {
        String escaped = javaExpr.replace("\\", "\\\\").replace("\"", "\\\"");
        return "NullGuard.of(" + javaExpr + ", \"" + escaped + "\")";
    }

    /**
     * Generates a {@code NullGuard.of(expr, "description")} call for the given VarHandle,
     * or returns the raw Java expression if the variable is marked as nullable via its
     * {@code "nullable"} metadata entry.
     *
     * <p>Nullable variables (e.g. the {@code killer} in {@code entity_death}) skip the
     * NullGuard wrapper so that a null value does not throw a {@link LumenRuntimeException}
     * at the guard site. Instead, the caller will see a normal {@code NullPointerException}
     * if the value is used without an {@code if X is set:} check, which the event runtime
     * catches and logs with a helpful message.
     *
     * @param handle the variable handle to guard
     * @return the guarded or raw Java expression
     */
    public static @NotNull String codegenOrRaw(@NotNull EnvironmentAccess.VarHandle handle) {
        if (handle.hasMeta("nullable")) {
            return handle.java();
        }
        return codegen(handle.java());
    }

    /**
     * Returns the value if it is non-null, otherwise throws a
     * {@link LumenRuntimeException} with the script source context.
     *
     * @param value   the value to check
     * @param varName the script variable name (for the error message)
     * @param line    the 1-based script line number
     * @param source  the raw script source text for the line
     * @param <T>     the value type
     * @return the non-null value
     * @throws LumenRuntimeException if the value is null
     */
    public static <T> @NotNull T of(@Nullable T value,
                                    @NotNull String varName,
                                    int line,
                                    @NotNull String source) {
        if (value == null) {
            throw new LumenRuntimeException(
                    "Variable '" + varName + "' is null (line " + line + ": " + source + ")",
                    line,
                    source);
        }
        return value;
    }

    /**
     * Returns the value if it is non-null, otherwise throws a
     * {@link LumenRuntimeException} indicating the variable is null.
     *
     * <p>This overload is used when the script line number is not available
     * (e.g. inside type binding code generation).
     *
     * @param value   the value to check
     * @param varName the script variable name (for the error message)
     * @param <T>     the value type
     * @return the non-null value
     * @throws LumenRuntimeException if the value is null
     */
    public static <T> @NotNull T of(@Nullable T value,
                                    @NotNull String varName) {
        if (value == null) {
            throw new LumenRuntimeException(
                    "Variable '" + varName + "' is null - check that it was properly initialized",
                    0,
                    "");
        }
        return value;
    }
}
