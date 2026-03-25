package dev.lumenlang.lumen.api.pattern.builder;

import dev.lumenlang.lumen.api.handler.ExpressionHandler;
import dev.lumenlang.lumen.api.inject.body.InjectableExpression;
import dev.lumenlang.lumen.api.pattern.Category;
import dev.lumenlang.lumen.api.pattern.PatternMeta;
import dev.lumenlang.lumen.api.pattern.PatternRegistrar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fluent builder for registering an expression pattern with documentation
 * metadata.
 *
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * api.patterns().expression(b -> b
 *         .pattern("[get] %who:PLAYER% location")
 *         .description("Returns the location of a player.")
 *         .example("var loc = get player location")
 *         .since("1.0.0")
 *         .category(Categories.PLAYER)
 *         .handler(ctx -> new ExpressionResult(ctx.java("who") + ".getLocation()", Types.LOCATION.id())));
 * }</pre>
 *
 * @see PatternRegistrar#expression(Consumer)
 */
public final class ExpressionBuilder {

    private final List<String> patterns = new ArrayList<>();
    private final List<String> examples = new ArrayList<>();
    private @Nullable String by;
    private @Nullable String description;
    private @Nullable String since;
    private @Nullable Category category;
    private boolean deprecated;
    private @Nullable String returnRefTypeId;
    private @Nullable String returnJavaType;
    private @Nullable ExpressionHandler handler;
    private @Nullable InjectableExpression injectableExpression;
    private @Nullable Class<?> injectableClass;
    private @Nullable String injectableMethodName;

    /**
     * Sets the addon name that registers this expression pattern.
     *
     * @param by the addon name (e.g. "Lumen")
     * @return this builder
     */
    public @NotNull ExpressionBuilder by(@NotNull String by) {
        this.by = by;
        return this;
    }

    /**
     * Adds a pattern string for this expression.
     *
     * @param pattern the expression pattern (e.g.
     *                {@code "[get] %who:PLAYER% location"})
     * @return this builder
     */
    public @NotNull ExpressionBuilder pattern(@NotNull String pattern) {
        this.patterns.add(pattern);
        return this;
    }

    /**
     * Adds multiple pattern strings that all use the same handler.
     *
     * @param patterns the expression patterns
     * @return this builder
     */
    public @NotNull ExpressionBuilder patterns(@NotNull String... patterns) {
        Collections.addAll(this.patterns, patterns);
        return this;
    }

    /**
     * Sets the human-readable description of what this expression returns.
     *
     * @param description the description
     * @return this builder
     */
    public @NotNull ExpressionBuilder description(@NotNull String description) {
        this.description = description;
        return this;
    }

    /**
     * Adds an example showing how this expression is used in a Lumen script.
     *
     * @param example a Lumen script example
     * @return this builder
     */
    public @NotNull ExpressionBuilder example(@NotNull String example) {
        this.examples.add(example);
        return this;
    }

    /**
     * Adds multiple examples showing how this expression is used in a Lumen script.
     *
     * @param examples the Lumen script examples
     * @return this builder
     */
    public @NotNull ExpressionBuilder examples(@NotNull String... examples) {
        Collections.addAll(this.examples, examples);
        return this;
    }

    /**
     * Sets the version in which this expression was introduced.
     *
     * @param since the version string (e.g. "1.0.0")
     * @return this builder
     */
    public @NotNull ExpressionBuilder since(@NotNull String since) {
        this.since = since;
        return this;
    }

    /**
     * Sets the documentation category for this expression.
     *
     * @param category the category
     * @return this builder
     */
    public @NotNull ExpressionBuilder category(@NotNull Category category) {
        this.category = category;
        return this;
    }

    /**
     * Marks this expression as deprecated.
     *
     * <p>
     * Deprecated patterns will still work but will be flagged
     * in documentation as patterns that should be avoided.
     *
     * @param deprecated true if this expression is deprecated
     * @return this builder
     */
    public @NotNull ExpressionBuilder deprecated(boolean deprecated) {
        this.deprecated = deprecated;
        return this;
    }

    /**
     * Declares the ref type this expression statically produces.
     *
     * <p>When set, tooling can resolve the type of a variable assigned from
     * this expression without executing the handler. Expressions whose return
     * type depends on runtime input should leave this unset ({@code null}).
     *
     * @param returnRefTypeId the ref type id (e.g. {@code Types.PLAYER.id()}), or {@code null}
     * @return this builder
     */
    public @NotNull ExpressionBuilder returnRefTypeId(@Nullable String returnRefTypeId) {
        this.returnRefTypeId = returnRefTypeId;
        return this;
    }

    /**
     * Declares the Java type this expression statically produces for primitive
     * or string results.
     *
     * <p>Use this for expressions that return values like {@code int},
     * {@code double}, or {@code String}. For object types that have a ref type
     * (e.g. PLAYER, LOCATION), use {@link #returnRefTypeId} instead.
     *
     * @param returnJavaType the Java type (e.g. {@code Types.INT}, {@code Types.STRING}), or {@code null}
     * @return this builder
     */
    public @NotNull ExpressionBuilder returnJavaType(@Nullable String returnJavaType) {
        this.returnJavaType = returnJavaType;
        return this;
    }

    /**
     * Sets the handler that returns the Java expression result.
     *
     * @param handler the expression handler
     * @return this builder
     */
    public @NotNull ExpressionBuilder handler(@NotNull ExpressionHandler handler) {
        this.handler = handler;
        return this;
    }

    /**
     * Sets an injectable expression whose bytecode will be extracted and injected
     * into the compiled script class. This is an alternative to {@link #handler}.
     *
     * <p>When using this, set {@link #returnRefTypeId} or {@link #returnJavaType}
     * to declare what type the expression produces.
     *
     * @param expression the injectable expression
     * @return this builder
     */
    public @NotNull ExpressionBuilder injectableHandler(@NotNull InjectableExpression expression) {
        this.injectableExpression = expression;
        return this;
    }

    /**
     * Sets a static method whose bytecode will be extracted and injected
     * into the compiled script class. This is an alternative to {@link #handler}.
     *
     * <p>When using this, set {@link #returnRefTypeId} or {@link #returnJavaType}
     * to declare what type the expression produces.
     *
     * @param clazz the class containing the static method
     * @param methodName the name of the static method
     * @return this builder
     */
    public @NotNull ExpressionBuilder injectableHandler(@NotNull Class<?> clazz, @NotNull String methodName) {
        this.injectableClass = clazz;
        this.injectableMethodName = methodName;
        return this;
    }

    public @NotNull List<String> getPatterns() {
        return patterns;
    }

    public @NotNull ExpressionHandler getHandler() {
        if (handler == null)
            throw new IllegalStateException("Expression handler is not set for pattern: " + patterns.get(0));
        return handler;
    }

    public @Nullable InjectableExpression getInjectableExpression() {
        return injectableExpression;
    }

    public @Nullable Class<?> getInjectableClass() {
        return injectableClass;
    }

    public @Nullable String getInjectableMethodName() {
        return injectableMethodName;
    }

    public @Nullable String getReturnRefTypeId() {
        return returnRefTypeId;
    }

    public @Nullable String getReturnJavaType() {
        return returnJavaType;
    }

    public @NotNull PatternMeta buildMeta() {
        return new PatternMeta(by, description, List.copyOf(examples), since, category, deprecated);
    }

    public void validate() {
        if (patterns.isEmpty()) {
            throw new IllegalStateException("Expression builder requires at least one pattern");
        }
        if (handler == null && injectableExpression == null && injectableClass == null) {
            throw new IllegalStateException("Expression builder requires a handler or injectableHandler");
        }
        if (handler != null && (injectableExpression != null || injectableClass != null)) {
            throw new IllegalStateException("Only one of handler or injectableHandler may be set");
        }
        if (by == null) {
            throw new IllegalStateException("Expression builder requires a 'by' (addon name)");
        }
        if (returnRefTypeId != null && returnJavaType != null) {
            throw new IllegalStateException("Only one of returnRefTypeId or returnJavaType may be set");
        }
    }
}
