package dev.lumenlang.lumen.api.pattern.builder;

import dev.lumenlang.lumen.api.handler.StatementHandler;
import dev.lumenlang.lumen.api.inject.body.InjectableBody;
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
 * Fluent builder for registering a statement pattern with documentation
 * metadata.
 *
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * api.patterns().statement(b -> b
 *         .pattern("message %who:PLAYER% %text:STRING%")
 *         .description("Sends a message to a player.")
 *         .example("message player \"Hello!\"")
 *         .since("1.0.0")
 *         .category(Categories.PLAYER)
 *         .handler(ctx -> ctx.out().line(ctx.java("who") + ".sendMessage(" + ctx.java("text") + ");")));
 * }</pre>
 *
 * @see PatternRegistrar#statement(Consumer)
 */
public final class StatementBuilder {

    private final List<String> patterns = new ArrayList<>();
    private final List<String> examples = new ArrayList<>();
    private @Nullable String by;
    private @Nullable String description;
    private @Nullable String since;
    private @Nullable Category category;
    private boolean deprecated;
    private @Nullable StatementHandler handler;
    private @Nullable InjectableBody injectableBody;
    private @Nullable Class<?> injectableClass;
    private @Nullable String injectableMethodName;
    private boolean methodBased;

    /**
     * Sets the addon name that registers this statement pattern.
     *
     * @param by the addon name (e.g. "Lumen")
     * @return this builder
     */
    public @NotNull StatementBuilder by(@NotNull String by) {
        this.by = by;
        return this;
    }

    /**
     * Adds a pattern string for this statement.
     *
     * @param pattern the statement pattern (e.g.
     *                {@code "message %who:PLAYER% %text:STRING%"})
     * @return this builder
     */
    public @NotNull StatementBuilder pattern(@NotNull String pattern) {
        this.patterns.add(pattern);
        return this;
    }

    /**
     * Adds multiple pattern strings that all use the same handler.
     *
     * @param patterns the statement patterns
     * @return this builder
     */
    public @NotNull StatementBuilder patterns(@NotNull String... patterns) {
        Collections.addAll(this.patterns, patterns);
        return this;
    }

    /**
     * Sets the human-readable description of what this statement does.
     *
     * @param description the description
     * @return this builder
     */
    public @NotNull StatementBuilder description(@NotNull String description) {
        this.description = description;
        return this;
    }

    /**
     * Adds an example showing how this statement is used in a Lumen script.
     *
     * @param example a Lumen script example
     * @return this builder
     */
    public @NotNull StatementBuilder example(@NotNull String example) {
        this.examples.add(example);
        return this;
    }

    /**
     * Adds multiple examples showing how this statement is used in a Lumen script.
     *
     * @param examples the Lumen script examples
     * @return this builder
     */
    public @NotNull StatementBuilder examples(@NotNull String... examples) {
        Collections.addAll(this.examples, examples);
        return this;
    }

    /**
     * Sets the version in which this statement was introduced.
     *
     * @param since the version string (e.g. "1.0.0")
     * @return this builder
     */
    public @NotNull StatementBuilder since(@NotNull String since) {
        this.since = since;
        return this;
    }

    /**
     * Sets the documentation category for this statement.
     *
     * @param category the category
     * @return this builder
     */
    public @NotNull StatementBuilder category(@NotNull Category category) {
        this.category = category;
        return this;
    }

    /**
     * Marks this statement as deprecated.
     *
     * <p>
     * Deprecated patterns will still work but will be flagged
     * in documentation as patterns that should be avoided.
     *
     * @param deprecated true if this statement is deprecated
     * @return this builder
     */
    public @NotNull StatementBuilder deprecated(boolean deprecated) {
        this.deprecated = deprecated;
        return this;
    }

    /**
     * Sets the handler that generates Java code for this statement.
     *
     * @param handler the statement handler
     * @return this builder
     */
    public @NotNull StatementBuilder handler(@NotNull StatementHandler handler) {
        this.handler = handler;
        return this;
    }

    /**
     * Sets an injectable body whose bytecode will be extracted and injected
     * into the compiled script class. This is an alternative to {@link #handler}.
     *
     * @param body the injectable body
     * @return this builder
     */
    public @NotNull StatementBuilder injectableHandler(@NotNull InjectableBody body) {
        this.injectableBody = body;
        return this;
    }

    /**
     * Sets a static method whose bytecode will be extracted and injected
     * into the compiled script class. This is an alternative to {@link #handler}.
     *
     * @param clazz the class containing the static method
     * @param methodName the name of the static method
     * @return this builder
     */
    public @NotNull StatementBuilder injectableHandler(@NotNull Class<?> clazz, @NotNull String methodName) {
        this.injectableClass = clazz;
        this.injectableMethodName = methodName;
        return this;
    }

    /**
     * Forces method based injection instead of inline.
     *
     * <p>By default, injectable handlers use inline mode where the decompiled body
     * is emitted directly at the call site. Calling this forces the handler to
     * generate a bridge method with bytecode injection instead.
     *
     * @return this builder
     */
    public @NotNull StatementBuilder methodBased() {
        this.methodBased = true;
        return this;
    }

    public @NotNull List<String> getPatterns() {
        return patterns;
    }

    public @NotNull StatementHandler getHandler() {
        if (handler == null)
            throw new IllegalStateException("Statement handler cannot be null for pattern: " + patterns.get(0));
        return handler;
    }

    public @Nullable InjectableBody getInjectableBody() {
        return injectableBody;
    }

    public @Nullable Class<?> getInjectableClass() {
        return injectableClass;
    }

    public @Nullable String getInjectableMethodName() {
        return injectableMethodName;
    }

    public boolean isMethodBased() {
        return methodBased;
    }

    public @NotNull PatternMeta buildMeta() {
        return new PatternMeta(by, description, List.copyOf(examples), since, category, deprecated);
    }

    public void validate() {
        if (patterns.isEmpty()) {
            throw new IllegalStateException("Statement builder requires at least one pattern");
        }
        if (handler == null && injectableBody == null && injectableClass == null) {
            throw new IllegalStateException("Statement builder requires a handler or injectableHandler");
        }
        if (handler != null && (injectableBody != null || injectableClass != null)) {
            throw new IllegalStateException("Only one of handler or injectableHandler may be set");
        }
        if (injectableBody != null && injectableClass != null) {
            throw new IllegalStateException("Only one injectable source may be set: use either injectableHandler(InjectableBody) or injectableHandler(Class, String)");
        }
        if (by == null) {
            throw new IllegalStateException("Statement builder requires a 'by' (addon name)");
        }
    }
}
