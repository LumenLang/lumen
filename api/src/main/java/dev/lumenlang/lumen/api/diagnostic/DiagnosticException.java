package dev.lumenlang.lumen.api.diagnostic;

import org.jetbrains.annotations.NotNull;

/**
 * A runtime exception carrying a {@link LumenDiagnostic}.
 *
 * <p>When thrown during code generation, the pipeline catches this and displays the
 * formatted diagnostic directly.
 */
public final class DiagnosticException extends RuntimeException {

    private final @NotNull LumenDiagnostic diagnostic;

    public DiagnosticException(@NotNull LumenDiagnostic diagnostic) {
        super(diagnostic.format());
        this.diagnostic = diagnostic;
    }

    public @NotNull LumenDiagnostic diagnostic() {
        return diagnostic;
    }
}
