package net.vansencool.lumen.pipeline.codegen;

import net.vansencool.lumen.api.codegen.BindingAccess;
import net.vansencool.lumen.api.codegen.EnvironmentAccess;
import net.vansencool.lumen.api.handler.BlockHandler;
import net.vansencool.lumen.api.handler.StatementHandler;
import net.vansencool.lumen.pipeline.conditions.ConditionExpr;
import net.vansencool.lumen.pipeline.conditions.parser.ConditionParser;
import net.vansencool.lumen.pipeline.language.TypeBinding;
import net.vansencool.lumen.pipeline.language.match.BoundValue;
import net.vansencool.lumen.pipeline.language.match.BraceExpr;
import net.vansencool.lumen.pipeline.language.match.InlineExpr;
import net.vansencool.lumen.pipeline.language.match.Match;
import net.vansencool.lumen.pipeline.language.pattern.PatternRegistry;
import net.vansencool.lumen.pipeline.language.resolve.ExprResolver;
import net.vansencool.lumen.pipeline.language.tokenization.Token;
import net.vansencool.lumen.api.handler.ExpressionHandler.ExpressionResult;
import net.vansencool.lumen.pipeline.var.RefType;
import net.vansencool.lumen.pipeline.logger.LumenLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides access to matched pattern parameters during statement and block
 * handler execution.
 *
 * <p>
 * When a Lumen pattern like
 * {@code "give %who:PLAYER% %item:MATERIAL% %amt:INT%"} is matched
 * against script tokens, the resulting parameter bindings are stored in a
 * {@link Match} object. The BindingContext wraps this match
 * and provides convenient methods for handlers to extract:
 * <ul>
 * <li>Parsed values (via {@link #value(String)})</li>
 * <li>Generated Java code (via {@link #java(String)})</li>
 * <li>Original tokens (via {@link #bound(String)})</li>
 * <li>Environment and codegen context</li>
 * </ul>
 *
 * <h2>Usage in Statement Handlers</h2>
 *
 * <pre>{@code
 * reg.statements("give %who:PLAYER% %item:MATERIAL% %amt:INT%", (node, ctx, out) -> {
 *     String whoJava = ctx.java("who"); // "player"
 *     String itemJava = ctx.java("item"); // "org.bukkit.Material.DIAMOND"
 *     String amtJava = ctx.java("amt"); // "3"
 *
 *     out.line(whoJava + ".getInventory().addItem(new ItemStack(" +
 *             itemJava + ", " + amtJava + "));");
 * });
 * }</pre>
 *
 * <p>
 * The {@link #java(String)} method is the primary way handlers generate Java
 * code. It:
 * <ol>
 * <li>Retrieves the parsed value for the parameter</li>
 * <li>Calls the type binding's
 * {@link TypeBinding#toJava(Object, CodegenContext, TypeEnv)}</li>
 * <li>Returns the generated Java code as a string</li>
 * </ol>
 *
 * <h2>Debug Output</h2>
 * <p>
 * The {@link #java(String)} method includes debug output to help trace
 * parameter resolution
 * issues. When a parameter cannot be found or has unexpected values, check the
 * debug logs for
 * detailed information about what was parsed and how it was converted to Java
 * code.
 *
 * @see StatementHandler
 * @see BlockHandler
 * @see Match
 */
@SuppressWarnings("unused")
public final class BindingContext implements BindingAccess {
    private final Match match;
    private final TypeEnv env;
    private final CodegenContext ctx;
    private final BlockContext block;

    public BindingContext(
            Match match,
            TypeEnv env,
            CodegenContext ctx,
            BlockContext block) {
        this.match = match;
        this.env = env;
        this.ctx = ctx;
        this.block = block;
    }

    /**
     * Retrieves the bound value for the specified parameter name.
     *
     * <p>
     * The BoundValue contains:
     * <ul>
     * <li>The placeholders metadata (name and type)</li>
     * <li>The original tokens that were matched</li>
     * <li>The parsed runtime value</li>
     * <li>The type binding that parsed it</li>
     * </ul>
     *
     * @param n the parameter name from the pattern (e.g., "who", "item", "amt")
     * @return the bound value containing parse results and metadata
     */
    public BoundValue bound(String n) {
        return match.values().get(n);
    }

    /**
     * Generates Java code for the specified parameter.
     *
     * <p>
     * This is the primary method handlers use to convert pattern parameters into
     * Java code.
     * It delegates to the type binding's {@code toJava} method after retrieving the
     * parsed value.
     *
     * <p>
     * <b>Debug Output:</b> This method logs detailed information about the
     * parameter resolution:
     * <ul>
     * <li>Parameter name being resolved</li>
     * <li>Whether the bound value exists</li>
     * <li>The parsed value and its type</li>
     * <li>The generated Java code</li>
     * </ul>
     *
     * @param n the parameter name from the pattern (e.g., "who", "item")
     * @return Java source code representing this parameter's value
     * @throws RuntimeException if the parameter does not exist in the match or a
     *                          multi-token EXPR cannot be resolved
     */
    @Override
    public @NotNull String java(@NotNull String n) {
        LumenLogger.debug("BindingContext", "java() called for parameter: '" + n + "'");
        BoundValue v = bound(n);
        if (v == null) {
            LumenLogger.debug("BindingContext", "ERROR: BoundValue is null for '" + n + "'");
            throw new RuntimeException("No bound value for parameter: " + n);
        }
        LumenLogger.debug("BindingContext", "BoundValue found: value=" + v.value() + ", binding=" + v.binding().id());

        String result = match.java(n, ctx, env);
        LumenLogger.debug("BindingContext", "Generated Java for '" + n + "': '" + result + "'");
        return result;
    }

    /**
     * Retrieves the parsed runtime value for the specified parameter.
     *
     * <p>
     * Unlike {@link #java(String)}, this returns the actual parsed object before
     * Java
     * code generation. This is useful when handlers need to inspect or manipulate
     * the value.
     *
     * <p>
     * For example, in the "aliases" statement handler, the raw comma-separated
     * string is
     * retrieved and split:
     *
     * <pre>{@code
     * CommandMeta cmd = ctx.block().getEnvFromParents("cmd_meta");
     * String raw = ctx.value("list").toString();
     * for (String part : raw.split(",")) {
     *     String alias = part.trim();
     *     if (!alias.isEmpty())
     *         cmd.addAlias(alias);
     * }
     * }</pre>
     *
     * @param n the parameter name from the pattern
     * @return the parsed value (type depends on the type binding)
     */
    @Override
    public Object value(@NotNull String n) {
        return resolveDeferred(bound(n).value());
    }

    /**
     * Returns the type environment for variable and reference lookups.
     *
     * @return the current type environment
     */
    @Override
    public @NotNull TypeEnv env() {
        return env;
    }

    /**
     * Returns the code generation context for managing imports and class metadata.
     *
     * @return the current codegen context
     */
    @Override
    public @NotNull CodegenContext codegen() {
        return ctx;
    }

    /**
     * Returns the block context for accessing parent blocks and sibling statements.
     *
     * @return the current block context
     */
    @Override
    public @NotNull BlockContext block() {
        return block;
    }

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
    @Override
    public @Nullable String choice(int index) {
        return match.choice(index);
    }

    /**
     * Returns the bound value at the given positional index.
     *
     * <p>Index order corresponds to placeholders order in the pattern (left to right).
     *
     * @param index the zero-based index
     * @return the bound value
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public BoundValue boundAt(int index) {
        return match.boundAt(index);
    }

    /**
     * Generates Java code for the parameter at the given positional index.
     *
     * @param index the zero-based index
     * @return Java source code representing this parameter's value
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public String javaAt(int index) {
        return match.javaAt(index, ctx, env);
    }

    /**
     * Retrieves the parsed runtime value at the given positional index.
     *
     * @param index the zero-based index
     * @return the parsed value
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public Object valueAt(int index) {
        return resolveDeferred(match.valueAt(index));
    }

    /**
     * Returns the number of bound parameters in this match.
     *
     * @return the parameter count
     */
    @Override
    public int size() {
        return match.size();
    }

    @Override
    public @NotNull List<String> tokens(@NotNull String name) {
        BoundValue bv = bound(name);
        return bv.tokens().stream()
                .map(Token::text)
                .collect(Collectors.toList());
    }

    @Override
    public @NotNull String java(int index) {
        return javaAt(index);
    }

    @Override
    public @NotNull Object value(int index) {
        return valueAt(index);
    }

    @Override
    public @NotNull List<String> tokens(int index) {
        BoundValue bv = match.boundAt(index);
        return bv.tokens().stream()
                .map(Token::text)
                .collect(Collectors.toList());
    }

    @Override
    public @NotNull String parseCondition(@NotNull String paramName) {
        ConditionParser cp = new ConditionParser(PatternRegistry.instance().conditionRegistry());
        ConditionExpr expr = cp.parse(bound(paramName).tokens(), env);
        return expr.toJava(env, ctx);
    }

    private @NotNull Object resolveDeferred(@NotNull Object value) {
        if (value instanceof InlineExpr ie) {
            ExpressionResult result = ExprResolver.resolveWithType(ie.tokens(), ctx, env);
            if (result != null) return toSyntheticHandle(result);
        }
        if (value instanceof BraceExpr be) {
            ExpressionResult result = ExprResolver.resolveWithType(be.innerTokens(), ctx, env);
            if (result != null) return toSyntheticHandle(result);
        }
        return value;
    }

    private static EnvironmentAccess.@NotNull VarHandle toSyntheticHandle(@NotNull ExpressionResult result) {
        RefType refType = result.refTypeId() != null ? RefType.byId(result.refTypeId()) : null;
        return Match.syntheticHandle(result.java(), refType, result.metadata());
    }
}
