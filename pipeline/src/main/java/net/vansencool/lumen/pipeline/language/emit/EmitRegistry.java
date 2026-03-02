package net.vansencool.lumen.pipeline.language.emit;

import net.vansencool.lumen.api.emit.BlockEnterHook;
import net.vansencool.lumen.api.emit.BlockFormHandler;
import net.vansencool.lumen.api.emit.StatementFormHandler;
import net.vansencool.lumen.pipeline.language.pattern.PatternRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stores registered emit handlers (statement forms, block forms, and block enter hooks).
 *
 * <p>All built-in language features and addon-registered extensions are stored here.
 * The {@link CodeEmitter} consults this registry before falling back to pattern matching.
 *
 * <p>Uses the same static-singleton pattern as
 * {@link PatternRegistry}.
 */
public final class EmitRegistry {

    private static EmitRegistry INSTANCE;

    private final List<StatementFormHandler> statementForms = new ArrayList<>();
    private final List<BlockFormHandler> blockForms = new ArrayList<>();
    private final List<BlockEnterHook> blockEnterHooks = new ArrayList<>();

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
     * Registers a statement form handler.
     *
     * @param handler the handler to register
     */
    public void addStatementForm(@NotNull StatementFormHandler handler) {
        statementForms.add(handler);
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
     * Returns an unmodifiable view of all registered statement form handlers.
     *
     * @return the statement form handlers
     */
    public @NotNull List<StatementFormHandler> statementForms() {
        return Collections.unmodifiableList(statementForms);
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
}
