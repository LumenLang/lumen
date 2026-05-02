package dev.lumenlang.lumen.pipeline.language.emit;

import dev.lumenlang.lumen.api.emit.BlockEnterHook;
import dev.lumenlang.lumen.api.emit.BlockExitHook;
import dev.lumenlang.lumen.api.emit.BlockFormHandler;
import dev.lumenlang.lumen.api.emit.StatementValidator;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stores registered emit handlers (such as block forms and block enter hooks).
 */
public final class EmitRegistry {

    private static EmitRegistry INSTANCE;

    private final List<BlockFormHandler> blockForms = new ArrayList<>();
    private final List<BlockEnterHook> blockEnterHooks = new ArrayList<>();
    private final List<BlockExitHook> blockExitHooks = new ArrayList<>();
    private final List<StatementValidator> statementValidators = new ArrayList<>();

    /**
     * Sets the global singleton instance.
     *
     * @param instance the registry to use as the singleton
     */
    public static void instance(@NotNull EmitRegistry instance) {
        INSTANCE = instance;
    }

    /**
     * Returns the global singleton instance.
     *
     * @return the singleton
     * @throws IllegalStateException if not yet initialized
     */
    public static @NotNull EmitRegistry instance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("EmitRegistry has not been initialized");
        }
        return INSTANCE;
    }

    /**
     * Registers a block form handler.
     *
     * @param handler the handler to register
     */
    public void addBlockForm(@NotNull BlockFormHandler handler) {
        blockForms.add(handler);
    }

    /**
     * Registers a block enter hook.
     *
     * @param hook the hook to register
     */
    public void addBlockEnterHook(@NotNull BlockEnterHook hook) {
        blockEnterHooks.add(hook);
    }

    /**
     * Registers a block exit hook.
     *
     * @param hook the hook to register
     */
    public void addBlockExitHook(@NotNull BlockExitHook hook) {
        blockExitHooks.add(hook);
    }

    /**
     * Registers a statement validator.
     *
     * @param validator the validator to register
     */
    public void addStatementValidator(@NotNull StatementValidator validator) {
        statementValidators.add(validator);
    }

    /**
     * Returns an unmodifiable view of all registered block form handlers.
     *
     * @return the block form handlers
     */
    public @NotNull List<BlockFormHandler> blockForms() {
        return Collections.unmodifiableList(blockForms);
    }

    /**
     * Returns an unmodifiable view of all registered block enter hooks.
     *
     * @return the block enter hooks
     */
    public @NotNull List<BlockEnterHook> blockEnterHooks() {
        return Collections.unmodifiableList(blockEnterHooks);
    }

    /**
     * Returns an unmodifiable view of all registered block exit hooks.
     *
     * @return the block exit hooks
     */
    public @NotNull List<BlockExitHook> blockExitHooks() {
        return Collections.unmodifiableList(blockExitHooks);
    }

    /**
     * Returns an unmodifiable view of all registered statement validators.
     *
     * @return the statement validators
     */
    public @NotNull List<StatementValidator> statementValidators() {
        return Collections.unmodifiableList(statementValidators);
    }
}
