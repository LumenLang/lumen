package net.vansencool.lumen.pipeline.java.compiler.system;

import net.vansencool.lumen.pipeline.java.compiled.ScriptSourceMap;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Thrown when Java compilation of a generated script class fails.
 *
 * <p>Carries structured diagnostic information (the fully-qualified class name,
 * the Java source line number, and the compiler error message) so callers can
 * map each error back to the original script source line via
 * {@link ScriptSourceMap}.
 */
public final class CompilationFailedException extends RuntimeException {

    private final List<CompileError> errors;

    /**
     * Creates a new exception with the given list of compile errors.
     *
     * @param errors the individual compiler error diagnostics
     */
    public CompilationFailedException(@NotNull List<CompileError> errors) {
        super("Compilation failed with " + errors.size() + " error(s)");
        this.errors = List.copyOf(errors);
    }

    /**
     * Returns the list of individual compiler errors.
     *
     * @return the compile errors
     */
    public @NotNull List<CompileError> errors() {
        return errors;
    }

    /**
     * A single compiler diagnostic error.
     *
     * @param fqcn     the fully-qualified class name of the source file that triggered the error
     * @param javaLine the 1-based Java source line number, or -1 if not available
     * @param message  the compiler error message
     */
    public record CompileError(@NotNull String fqcn, long javaLine, @NotNull String message) {
    }
}
