package dev.lumenlang.lumen.api.emit;

import dev.lumenlang.lumen.api.codegen.CodegenAccess;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.codegen.JavaOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Context provided to emit handlers (statement forms, block forms, and block enter hooks)
 * during code generation.
 *
 * <p>Provides access to the compile-time environment, code generation utilities,
 * the Java output builder, and expression resolution.
 *
 * @see StatementFormHandler
 * @see BlockFormHandler
 * @see BlockEnterHook
 */
public interface EmitContext {

    /**
     * Returns the compile-time symbol table for variable lookups and definitions.
     *
     * @return the environment access
     */
    @NotNull EnvironmentAccess env();

    /**
     * Returns the code generation context for class-level operations.
     *
     * @return the codegen access
     */
    @NotNull CodegenAccess codegen();

    /**
     * Returns the Java source output builder.
     *
     * @return the output
     */
    @NotNull JavaOutput out();

    /**
     * Attempts to resolve a list of tokens into a Java expression string.
     *
     * <p>This tries registered expression patterns and variable lookups to produce
     * the corresponding Java code for the given tokens.
     *
     * @param tokens the expression tokens to resolve
     * @return the resolved Java expression, or {@code null} if unresolvable
     */
    @Nullable String resolveExpression(@NotNull List<? extends ScriptToken> tokens);

    /**
     * Returns the 1-based source line number of the current node being emitted.
     *
     * @return the source line number
     */
    int line();

    /**
     * Returns the raw source text of the current node being emitted.
     *
     * @return the raw source text
     */
    @NotNull String raw();
}
