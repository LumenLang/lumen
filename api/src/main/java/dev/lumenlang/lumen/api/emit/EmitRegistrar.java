package dev.lumenlang.lumen.api.emit;

import org.jetbrains.annotations.NotNull;

/**
 * Registrar for custom emit handlers that extend the code generation pipeline.
 *
 * <p>All built-in language features (variables, stored variables, global variables,
 * config blocks, data blocks) are registered through this same interface, giving
 * addons the same capabilities as the core language.
 *
 * <p>Statement form handlers and block form handlers are tried in registration order
 * before the pattern matching system. Block enter hooks run for every pattern-matched
 * block.
 *
 * @see StatementFormHandler
 * @see BlockFormHandler
 * @see BlockEnterHook
 */
public interface EmitRegistrar {

    /**
     * Registers a statement form handler.
     *
     * <p>Handlers are tried in registration order before pattern matching.
     *
     * @param handler the handler to register
     */
    void statementForm(@NotNull StatementFormHandler handler);

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
}
