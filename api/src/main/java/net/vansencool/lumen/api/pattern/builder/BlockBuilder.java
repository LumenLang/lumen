package net.vansencool.lumen.api.pattern.builder;

import net.vansencool.lumen.api.handler.BlockHandler;
import net.vansencool.lumen.api.pattern.Category;
import net.vansencool.lumen.api.pattern.PatternMeta;
import net.vansencool.lumen.api.pattern.PatternRegistrar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fluent builder for registering a block pattern with documentation metadata.
 *
 * <p>
 * Example usage:
 *
 * <pre>{@code
 * api.patterns().block(b -> b
 *         .pattern("if %cond:EXPR%")
 *         .description("Executes the indented block if the condition is true.")
 *         .example("if player is sneaking:")
 *         .since("1.0.0")
 *         .category(Categories.CONTROL_FLOW)
 *         .handler(new BlockHandler() {
 *             public void begin(BindingAccess ctx, JavaOutput out) {
 *                 out.line("if (" + ctx.parseCondition("cond") + ") {");
 *             }
 *
 *             public void end(BindingAccess ctx, JavaOutput out) {
 *                 out.line("}");
 *             }
 *         }));
 * }</pre>
 *
 * @see PatternRegistrar#block(Consumer)
 */
public final class BlockBuilder {

    private final List<String> patterns = new ArrayList<>();
    private final List<String> examples = new ArrayList<>();
    private @Nullable String by;
    private @Nullable String description;
    private @Nullable String since;
    private @Nullable Category category;
    private boolean deprecated;
    private @Nullable BlockHandler handler;

    /**
     * Sets the addon name that registers this block pattern.
     *
     * @param by the addon name (e.g. "Lumen")
     * @return this builder
     */
    public @NotNull BlockBuilder by(@NotNull String by) {
        this.by = by;
        return this;
    }

    /**
     * Adds a pattern string for this block.
     *
     * @param pattern the block pattern (e.g. {@code "if %cond:EXPR%"})
     * @return this builder
     */
    public @NotNull BlockBuilder pattern(@NotNull String pattern) {
        this.patterns.add(pattern);
        return this;
    }

    /**
     * Adds multiple pattern strings that all use the same handler.
     *
     * @param patterns the block patterns
     * @return this builder
     */
    public @NotNull BlockBuilder patterns(@NotNull String... patterns) {
        Collections.addAll(this.patterns, patterns);
        return this;
    }

    /**
     * Sets the human-readable description of what this block does.
     *
     * @param description the description
     * @return this builder
     */
    public @NotNull BlockBuilder description(@NotNull String description) {
        this.description = description;
        return this;
    }

    /**
     * Adds an example showing how this block is used in a Lumen script.
     *
     * @param example a Lumen script example
     * @return this builder
     */
    public @NotNull BlockBuilder example(@NotNull String example) {
        this.examples.add(example);
        return this;
    }

    /**
     * Adds multiple examples showing how this block is used in a Lumen script.
     *
     * @param examples the Lumen script examples
     * @return this builder
     */
    public @NotNull BlockBuilder examples(@NotNull String... examples) {
        Collections.addAll(this.examples, examples);
        return this;
    }

    /**
     * Sets the version in which this block was introduced.
     *
     * @param since the version string (e.g. "1.0.0")
     * @return this builder
     */
    public @NotNull BlockBuilder since(@NotNull String since) {
        this.since = since;
        return this;
    }

    /**
     * Sets the documentation category for this block.
     *
     * @param category the category
     * @return this builder
     */
    public @NotNull BlockBuilder category(@NotNull Category category) {
        this.category = category;
        return this;
    }

    /**
     * Marks this block as deprecated.
     *
     * <p>
     * Deprecated patterns will still work but will be flagged
     * in documentation as patterns that should be avoided.
     *
     * @param deprecated true if this block is deprecated
     * @return this builder
     */
    public @NotNull BlockBuilder deprecated(boolean deprecated) {
        this.deprecated = deprecated;
        return this;
    }

    /**
     * Sets the handler that generates Java code for the block's begin and end.
     *
     * @param handler the block handler
     * @return this builder
     */
    public @NotNull BlockBuilder handler(@NotNull BlockHandler handler) {
        this.handler = handler;
        return this;
    }

    public @NotNull List<String> getPatterns() {
        return patterns;
    }

    public @NotNull BlockHandler getHandler() {
        if (handler == null)
            throw new IllegalStateException("Block handler is required for pattern: " + patterns.get(0));
        return handler;
    }

    public @NotNull PatternMeta buildMeta() {
        return new PatternMeta(by, description, List.copyOf(examples), since, category, deprecated);
    }

    public void validate() {
        if (patterns.isEmpty()) {
            throw new IllegalStateException("Block builder requires at least one pattern");
        }
        if (handler == null) {
            throw new IllegalStateException("Block builder requires a handler");
        }
        if (by == null) {
            throw new IllegalStateException("Block builder requires a 'by' (addon name)");
        }
    }
}
