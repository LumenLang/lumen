package dev.lumenlang.lumen.pipeline.language.match;

import dev.lumenlang.lumen.api.codegen.CodegenAccess;
import dev.lumenlang.lumen.api.codegen.EnvironmentAccess;
import dev.lumenlang.lumen.api.handler.ConditionHandler;
import dev.lumenlang.lumen.api.handler.ExpressionHandler.ExpressionResult;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.LumenTypeRegistry;
import dev.lumenlang.lumen.api.type.ObjectType;
import dev.lumenlang.lumen.pipeline.codegen.CodegenContext;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.language.exceptions.TokenCarryingException;
import dev.lumenlang.lumen.pipeline.language.pattern.Pattern;
import dev.lumenlang.lumen.pipeline.language.resolve.ExprResolver;
import dev.lumenlang.lumen.pipeline.var.VarRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Represents a successful pattern match with all bound parameters.
 *
 * @param pattern the pattern that was matched
 * @param values  map of parameter names to their bound values
 * @param choices text of the matched alternative for each required choice group, in order
 */
public record Match(
        @NotNull Pattern pattern,
        @NotNull Map<String, BoundValue> values,
        @NotNull List<String> choices
) implements ConditionHandler.ConditionMatch {

    /**
     * Resolves a bound value to Java source code. For EXPR-typed bindings with
     * multiple tokens, attempts expression inlining via the pattern registry.
     *
     * @param bv  the bound value to resolve
     * @param ctx the code generation context
     * @param env the type environment
     * @return the generated Java source expression
     */
    private static @NotNull String resolveBinding(
            @NotNull BoundValue bv,
            @NotNull CodegenContext ctx,
            @NotNull TypeEnv env) {
        if (bv.value() instanceof BraceExpr be) {
            String inlined = ExprResolver.resolve(be.innerTokens(), ctx, env);
            if (inlined != null) return inlined;
            throw new TokenCarryingException(
                    "Could not resolve braced expression: '{"
                            + ExprResolver.joinTokens(be.innerTokens()) + "}'",
                    bv.tokens());
        }
        if (bv.value() instanceof InlineExpr ie) {
            ExpressionResult result = ExprResolver.resolveWithType(ie.tokens(), ctx, env);
            if (result != null) {
                if (!bv.binding().id().equals("EXPR")) {
                    return bv.binding().toJava(
                            new InlineVarRef(result.java(), result.type(), result.metadata()), ctx, env);
                }
                return result.java();
            }
            throw new TokenCarryingException(
                    "Could not resolve inline expression: '"
                            + ExprResolver.joinTokens(ie.tokens()) + "'",
                    bv.tokens());
        }
        if (bv.binding().id().equals("EXPR")) {
            String inlined = ExprResolver.resolve(bv.tokens(), ctx, env);
            if (inlined != null) return inlined;
            if (bv.tokens().size() > 1) {
                throw new TokenCarryingException(
                        "Expression not recognized: '" + ExprResolver.joinTokens(bv.tokens())
                                + "'. Check spelling of variables and expression patterns.",
                        bv.tokens());
            }
        }
        return bv.binding().toJava(bv.value(), ctx, env);
    }

    /**
     * Returns the matched alternative text for the Nth required choice group in the
     * pattern.
     *
     * @param index the zero-based index of the required choice group
     * @return the matched alternative text, or null if the index is out of range
     */
    public @Nullable String choice(int index) {
        return index >= 0 && index < choices.size() ? choices.get(index) : null;
    }

    /**
     * Returns the parsed value for the named parameter, cast to the expected type.
     *
     * @param name the parameter name
     * @param <T>  the expected type
     * @return the parsed value
     */
    @SuppressWarnings("unchecked")
    public <T> @NotNull T value(@NotNull String name) {
        return (T) values.get(name).value();
    }

    /**
     * Returns the parsed value for the named parameter as a {@link VarRef}.
     *
     * @param name the parameter name
     * @return the {@link VarRef}
     */
    public @NotNull VarRef ref(@NotNull String name) {
        return (VarRef) values.get(name).value();
    }

    /**
     * Converts the bound value for the named parameter into a Java source expression.
     *
     * @param name the parameter name
     * @param ctx  the code generation context
     * @param env  the type environment
     * @return the generated Java source expression
     */
    public @NotNull String java(@NotNull String name, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
        BoundValue bv = values.get(name);
        return resolveBinding(bv, ctx, env);
    }

    /**
     * Returns the number of bound parameters in this match.
     *
     * @return the parameter count
     */
    public int size() {
        return values.size();
    }

    /**
     * Returns the bound value at the given positional index.
     *
     * @param index the zero-based index
     * @return the bound value
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public @NotNull BoundValue boundAt(int index) {
        int i = 0;
        for (BoundValue bv : values.values()) {
            if (i == index)
                return bv;
            i++;
        }
        throw new IndexOutOfBoundsException(
                "Index " + index + " out of range for match with " + values.size() + " parameters");
    }

    /**
     * Returns the parsed value at the given positional index, cast to the expected type.
     *
     * @param index the zero-based index
     * @param <T>   the expected type
     * @return the parsed value
     */
    @SuppressWarnings("unchecked")
    public <T> @NotNull T valueAt(int index) {
        return (T) boundAt(index).value();
    }

    /**
     * Returns the parsed value at the given positional index as a {@link VarRef}.
     *
     * @param index the zero-based index
     * @return the {@link VarRef}
     */
    public @NotNull VarRef refAt(int index) {
        return (VarRef) boundAt(index).value();
    }

    /**
     * Converts the bound value at the given positional index into a Java source expression.
     *
     * @param index the zero-based index
     * @param ctx   the code generation context
     * @param env   the type environment
     * @return the generated Java source expression
     */
    public @NotNull String javaAt(int index, @NotNull CodegenContext ctx, @NotNull TypeEnv env) {
        BoundValue bv = boundAt(index);
        return resolveBinding(bv, ctx, env);
    }

    @Override
    public @NotNull String java(@NotNull String name,
                                @NotNull CodegenAccess ctx,
                                @NotNull EnvironmentAccess env) {
        return java(name, (CodegenContext) ctx, (TypeEnv) env);
    }

    @Override
    public <T> @NotNull T value(int index) {
        return valueAt(index);
    }

    @Override
    public EnvironmentAccess.@NotNull VarHandle ref(int index) {
        return refAt(index);
    }

    @Override
    public @NotNull String java(int index,
                                @NotNull CodegenAccess ctx,
                                @NotNull EnvironmentAccess env) {
        return javaAt(index, (CodegenContext) ctx, (TypeEnv) env);
    }

    /**
     * Creates a synthetic {@link EnvironmentAccess.VarHandle} wrapping a resolved Java
     * expression together with type and metadata from the original expression result.
     *
     * @param javaExpr  the resolved Java expression
     * @param type      the type of the expression, or {@code null}
     * @param metadata  compile-time metadata from the expression result
     * @return a VarHandle backed by the expression with full type info
     */
    public static EnvironmentAccess.@NotNull VarHandle syntheticHandle(
            @NotNull String javaExpr,
            @Nullable LumenType type,
            @NotNull Map<String, Object> metadata) {
        return new InlineVarRef(javaExpr, type, metadata);
    }

    /**
     * Synthetic {@link EnvironmentAccess.VarHandle} wrapping a resolved inline expression's
     * Java code. This allows typed bindings (e.g. MATERIAL, ENTITY_TYPE) to apply their
     * coercion logic (such as {@code Material.valueOf(...)}) when the slot was filled by
     * an inline expression rather than a direct variable reference.
     */
    private record InlineVarRef(
            @NotNull String javaExpr,
            @Nullable LumenType lumenType,
            @NotNull Map<String, Object> meta
    ) implements EnvironmentAccess.VarHandle {

        @Override
        public @Nullable LumenType type() {
            return lumenType;
        }

        @Override
        public @NotNull String java() {
            return javaExpr;
        }

        @Override
        public @Nullable Object meta(@NotNull String key) {
            return meta.get(key);
        }

        @Override
        public boolean hasMeta(@NotNull String key) {
            return meta.containsKey(key);
        }

        @Override
        public @NotNull Map<String, Object> metadata() {
            return meta;
        }
    }
}
