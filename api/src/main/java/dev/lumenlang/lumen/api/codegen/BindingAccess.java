package dev.lumenlang.lumen.api.codegen;

import dev.lumenlang.lumen.api.codegen.EnvironmentAccess.VarHandle;
import dev.lumenlang.lumen.api.handler.BlockHandler;
import dev.lumenlang.lumen.api.handler.StatementHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Provides access to matched pattern parameters during handler execution.
 *
 * <p>
 * When a Lumen pattern like
 * {@code "give %who:PLAYER% %item:MATERIAL% %amt:INT%"} matches
 * script tokens, the resulting parameter bindings are made available through
 * this interface.
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * api.patterns().statement("give %who:PLAYER% %item:MATERIAL% %amt:INT%", (line, ctx, out) -> {
 *     String whoJava = ctx.java("who"); // "player"
 *     String itemJava = ctx.java("item"); // "org.bukkit.Material.DIAMOND"
 *     String amtJava = ctx.java("amt"); // "3"
 *     out.line(whoJava + ".getInventory().addItem(new ItemStack(" +
 *             itemJava + ", " + amtJava + "));");
 * });
 * }</pre>
 *
 * @see StatementHandler
 * @see BlockHandler
 */
@SuppressWarnings("unused")
public interface BindingAccess {

    /**
     * Generates Java code for the specified parameter.
     *
     * <p>
     * Delegates to the matched type binding's {@code toJava} method.
     *
     * @param name the parameter name from the pattern (e.g. {@code "who"},
     *             {@code "item"})
     * @return Java source code representing this parameter's value
     */
    @NotNull
    String java(@NotNull String name);

    /**
     * Retrieves the parsed runtime value for the specified parameter.
     *
     * @param name the parameter name from the pattern
     * @return the parsed value (type depends on the type binding)
     */
    @Nullable
    Object value(@NotNull String name);

    /**
     * Returns the original tokens that were consumed for the specified parameter.
     *
     * @param name the parameter name from the pattern
     * @return the token texts
     */
    @NotNull
    List<String> tokens(@NotNull String name);

    /**
     * Returns the type environment for variable and reference lookups.
     *
     * @return the environment access
     */
    @NotNull
    EnvironmentAccess env();

    /**
     * Returns the code generation context for managing imports and class metadata.
     *
     * @return the codegen access
     */
    @NotNull
    CodegenAccess codegen();

    /**
     * Returns the block context for inspecting sibling blocks and scope position.
     *
     * @return the block access
     */
    @NotNull
    BlockAccess block();

    /**
     * Returns the matched alternative text for the Nth required choice group in the
     * pattern.
     *
     * <p>
     * Only required groups ({@code (a|b|c)}) are tracked. Optional groups
     * ({@code [...]})
     * are not included.
     *
     * @param index the zero-based index of the required choice group
     * @return the matched alternative text, or null if out of range
     */
    @Nullable
    String choice(int index);

    /**
     * Generates Java code for the parameter at the given positional index.
     *
     * <p>Index order corresponds to the order in which placeholders appear in
     * the pattern (left to right).
     *
     * @param index the zero-based index
     * @return Java source code representing this parameter's value
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @NotNull
    String java(int index);

    /**
     * Retrieves the parsed runtime value at the given positional index.
     *
     * @param index the zero-based index
     * @return the parsed value
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @NotNull
    Object value(int index);

    /**
     * Returns the original tokens that were consumed for the parameter at
     * the given positional index.
     *
     * @param index the zero-based index
     * @return the token texts
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @NotNull
    List<String> tokens(int index);

    /**
     * Returns the number of bound parameters in this match.
     *
     * @return the parameter count
     */
    int size();

    /**
     * Returns the parsed value for the named parameter as a {@link VarHandle}.
     *
     * <p>This is a convenience method that casts the result of {@link #value(String)}
     * to {@code VarHandle}, avoiding the manual cast that would otherwise be required
     * in handler code.
     *
     * @param name the parameter name from the pattern (e.g. {@code "e"})
     * @return the variable handle, or null if the value is null or not a VarHandle
     */
    default @Nullable VarHandle varHandle(@NotNull String name) {
        Object v = value(name);
        return v instanceof VarHandle h ? h : null;
    }

    /**
     * Returns the parsed value for the named parameter as a {@link VarHandle},
     * throwing if it is not available.
     *
     * <p>Use this when the parameter is guaranteed to be a variable reference
     * (e.g. PLAYER, ENTITY type bindings always produce VarHandle values).
     *
     * @param name the parameter name from the pattern (e.g. {@code "e"})
     * @return the variable handle, never null
     * @throws IllegalStateException if the value is null or not a VarHandle
     */
    default @NotNull VarHandle requireVarHandle(@NotNull String name) {
        Object v = value(name);
        if (v instanceof VarHandle h) return h;
        throw new IllegalStateException("Expected VarHandle for parameter '" + name
                + "', got: " + (v == null ? "null" : v.getClass().getSimpleName()));
    }

    /**
     * Parses the condition tokens from the named parameter through the condition registry
     * and returns a Java boolean expression string.
     *
     * <p>This is a convenience method for block handlers that need to evaluate conditions
     * (e.g. {@code if %cond:EXPR%}).
     *
     * @param paramName the name of the parameter containing condition tokens
     * @return a valid Java boolean expression
     */
    @NotNull String parseCondition(@NotNull String paramName);
}
