package dev.lumenlang.lumen.api.codegen;

import dev.lumenlang.lumen.api.codegen.TypeEnv.VarHandle;
import dev.lumenlang.lumen.api.emit.ScriptToken;
import dev.lumenlang.lumen.api.handler.BlockHandler;
import dev.lumenlang.lumen.api.handler.ConditionHandler;
import dev.lumenlang.lumen.api.handler.ExpressionHandler;
import dev.lumenlang.lumen.api.handler.LoopHandler;
import dev.lumenlang.lumen.api.handler.StatementHandler;
import dev.lumenlang.lumen.api.type.LumenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Unified context provided to all handlers during code generation.
 *
 * <p>
 * Every handler type ({@link StatementHandler}, {@link ExpressionHandler},
 * {@link ConditionHandler}, {@link BlockHandler}, {@link LoopHandler}) receives a
 * single {@code HandlerContext} that provides access to matched pattern parameters,
 * the compile-time environment, code generation utilities, and the Java output builder.
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * api.patterns().statement("give %who:PLAYER% %item:MATERIAL% %amt:INT%", ctx -> {
 *     String whoJava = ctx.java("who");
 *     String itemJava = ctx.java("item");
 *     String amtJava = ctx.java("amt");
 *     ctx.out().line(whoJava + ".getInventory().addItem(new ItemStack(" +
 *             itemJava + ", " + amtJava + "));");
 * });
 * }</pre>
 */
@SuppressWarnings("unused")
public interface HandlerContext {

    /**
     * Generates Java code for the specified parameter.
     *
     * <p>
     * Delegates to the matched type binding's {@code toJava} method.
     *
     * @param name the parameter name from the pattern (e.g. {@code "who"}, {@code "item"})
     * @return Java source code representing this parameter's value
     */
    @NotNull String java(@NotNull String name);

    /**
     * Generates Java code for the specified parameter, widened or narrowed to a target type.
     *
     * <p>
     * Statement and form handlers should call this overload whenever the target slot has a known
     * declared type (map value, list element, var declaration, scoped store). The returned Java
     * expression includes any necessary widening cast (e.g. {@code int} to {@code long}) so that
     * the caller never has to remember to widen by hand. If no widening is needed, returns the
     * same string as {@link #java(String)}.
     *
     * @param name       the parameter name from the pattern
     * @param targetType the type the value will be assigned into
     * @return Java source code, cast to the target type if necessary
     */
    @NotNull String java(@NotNull String name, @NotNull LumenType targetType);

    /**
     * Retrieves the parsed runtime value for the specified parameter.
     *
     * @param name the parameter name from the pattern
     * @return the parsed value (type depends on the type binding)
     */
    @Nullable Object value(@NotNull String name);

    /**
     * Returns the original tokens that were consumed for the specified parameter.
     *
     * @param name the parameter name from the pattern
     * @return the token texts
     */
    @NotNull List<String> tokens(@NotNull String name);

    /**
     * Returns the original {@link ScriptToken} objects consumed for the specified parameter,
     * preserving source positions and token types for use in rich diagnostics.
     *
     * @param name the parameter name from the pattern
     * @return the script tokens
     */
    @NotNull List<? extends ScriptToken> scriptTokens(@NotNull String name);

    /**
     * Returns the type environment for variable and reference lookups.
     *
     * @return the environment access
     */
    @NotNull TypeEnv env();

    /**
     * Returns the code generation context for managing imports and class metadata.
     *
     * @return the codegen access
     */
    @NotNull CodegenContext codegen();

    /**
     * Returns the block context for inspecting sibling blocks and scope position.
     *
     * @return the block access
     */
    @NotNull BlockContext block();

    /**
     * Returns the Java source output builder.
     *
     * @return the output
     */
    @NotNull JavaOutput out();

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

    /**
     * Returns the matched alternative text for the Nth required choice group in the pattern.
     *
     * <p>
     * Only required groups ({@code (a|b|c)}) are tracked. Optional groups ({@code [...]})
     * are not included.
     *
     * @param index the zero-based index of the required choice group
     * @return the matched alternative text, or null if out of range
     */
    @Nullable String choice(int index);

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
    @NotNull String java(int index);

    /**
     * Retrieves the parsed runtime value at the given positional index.
     *
     * @param index the zero-based index
     * @return the parsed value
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @NotNull Object value(int index);

    /**
     * Returns the original tokens that were consumed for the parameter at
     * the given positional index.
     *
     * @param index the zero-based index
     * @return the token texts
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @NotNull List<String> tokens(int index);

    /**
     * Returns the original {@link ScriptToken} objects consumed for the parameter at
     * the given positional index, preserving source positions and token types.
     *
     * @param index the zero-based index
     * @return the script tokens
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @NotNull List<? extends ScriptToken> scriptTokens(int index);

    /**
     * Returns the number of bound parameters in this match.
     *
     * @return the parameter count
     */
    int size();

    /**
     * Resolves the {@link LumenType} for the specified parameter by inspecting
     * the parsed value and, if necessary, resolving expression tokens.
     *
     * <p>If the parameter value is already a {@link VarHandle} (e.g. from LIST, MAP,
     * or PLAYER bindings), its type is returned directly. Otherwise, the original
     * tokens are resolved as an expression to determine the resulting type.
     *
     * @param name the parameter name from the pattern
     * @return the resolved type, or {@code null} if the type cannot be determined
     */
    @Nullable LumenType resolvedType(@NotNull String name);

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
     * Parses the condition tokens from the named parameter through the condition registry
     * and returns a Java boolean expression string.
     *
     * @param paramName the name of the parameter containing condition tokens
     * @return a valid Java boolean expression
     */
    @NotNull String parseCondition(@NotNull String paramName);

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
     * <p>Use this when the parameter is guaranteed to be a variable reference.
     *
     * @param name the parameter name from the pattern (e.g. {@code "e"})
     * @return the variable handle, never null
     * @throws IllegalStateException if the value is null or not a VarHandle
     */
    default @NotNull VarHandle requireVarHandle(@NotNull String name) {
        Object v = value(name);
        if (v instanceof VarHandle h) return h;
        throw new IllegalStateException("Expected VarHandle for parameter '" + name + "', got: " + (v == null ? "null" : v.getClass().getSimpleName()));
    }
}
