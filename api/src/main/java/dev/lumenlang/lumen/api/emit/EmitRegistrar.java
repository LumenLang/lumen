package dev.lumenlang.lumen.api.emit;

import org.jetbrains.annotations.NotNull;

/**
 * Registrar for custom emit handlers that extend the code generation pipeline.
 *
 * <p>Block form handlers are tried in registration order before the pattern matching
 * system. Block enter hooks run for every pattern-matched block.
 */
public interface EmitRegistrar {

    /**
     * Registers a block form handler.
     *
     * <p>Handlers are tried in registration order before block pattern matching.
     *
     * @param handler the handler to register
     */
    void blockForm(@NotNull BlockFormHandler handler);

    /**
     * Registers a block enter hook.
     *
     * <p>Hooks run after a pattern-matched block's {@code begin} and before its children.
     *
     * @param hook the hook to register
     */
    void blockEnterHook(@NotNull BlockEnterHook hook);

    /**
     * Registers a statement validator.
     *
     * @param validator the validator to register
     */
    void statementValidator(@NotNull StatementValidator validator);
}
