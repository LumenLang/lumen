package net.vansencool.lumen.api.pattern.builder;

import net.vansencool.lumen.api.handler.ConditionHandler;
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
 * Fluent builder for registering a condition pattern with documentation
 * metadata.
 *
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * api.patterns().condition(b -> b
 *         .pattern("%p:PLAYER% is sneaking")
 *         .description("Checks whether a player is currently sneaking.")
 *         .example("if player is sneaking:")
 *         .since("1.0.0")
 *         .category(Categories.PLAYER)
 *         .handler((match, env, ctx) -> match.ref("p").java() + ".isSneaking()"));
 * }</pre>
 *
 * @see PatternRegistrar#condition(Consumer)
 */
public final class ConditionBuilder {

    private final List<String> patterns = new ArrayList<>();
    private @Nullable String by;
    private @Nullable String description;
    private final List<String> examples = new ArrayList<>();
    private @Nullable String since;
    private @Nullable Category category;
    private boolean deprecated;
    private @Nullable ConditionHandler handler;

    /**
     * Sets the addon name that registers this condition pattern.
     *
     * @param by the addon name (e.g. "Lumen")
     * @return this builder
     */
    public @NotNull ConditionBuilder by(@NotNull String by) {
        this.by = by;
        return this;
    }

    /**
     * Adds a pattern string for this condition.
     *
     * @param pattern the condition pattern (e.g. {@code "%p:PLAYER% is sneaking"})
     * @return this builder
     */
    public @NotNull ConditionBuilder pattern(@NotNull String pattern) {
        this.patterns.add(pattern);
        return this;
    }

    /**
     * Adds multiple pattern strings that all use the same handler.
     *
     * @param patterns the condition patterns
     * @return this builder
     */
    public @NotNull ConditionBuilder patterns(@NotNull String... patterns) {
        Collections.addAll(this.patterns, patterns);
        return this;
    }

    /**
     * Sets the human-readable description of what this condition checks.
     *
     * @param description the description
     * @return this builder
     */
    public @NotNull ConditionBuilder description(@NotNull String description) {
        this.description = description;
        return this;
    }

    /**
     * Adds an example showing how this condition is used in a Lumen script.
     *
     * @param example a Lumen script example
     * @return this builder
     */
    public @NotNull ConditionBuilder example(@NotNull String example) {
        this.examples.add(example);
        return this;
    }

    /**
     * Adds multiple examples showing how this condition is used in a Lumen script.
     *
     * @param examples the Lumen script examples
     * @return this builder
     */
    public @NotNull ConditionBuilder examples(@NotNull String... examples) {
        Collections.addAll(this.examples, examples);
        return this;
    }

    /**
     * Sets the version in which this condition was introduced.
     *
     * @param since the version string (e.g. "1.0.0")
     * @return this builder
     */
    public @NotNull ConditionBuilder since(@NotNull String since) {
        this.since = since;
        return this;
    }

    /**
     * Sets the documentation category for this condition.
     *
     * @param category the category
     * @return this builder
     */
    public @NotNull ConditionBuilder category(@NotNull Category category) {
        this.category = category;
        return this;
    }

    /**
     * Marks this condition as deprecated.
     *
     * <p>
     * Deprecated patterns will still work but will be flagged
     * in documentation as patterns that should be avoided.
     *
     * @param deprecated true if this condition is deprecated
     * @return this builder
     */
    public @NotNull ConditionBuilder deprecated(boolean deprecated) {
        this.deprecated = deprecated;
        return this;
    }

    /**
     * Sets the handler that generates the Java boolean expression for this
     * condition.
     *
     * @param handler the condition handler
     * @return this builder
     */
    public @NotNull ConditionBuilder handler(@NotNull ConditionHandler handler) {
        this.handler = handler;
        return this;
    }

    public @NotNull List<String> getPatterns() {
        return patterns;
    }

    public @NotNull ConditionHandler getHandler() {
        if (handler == null)
            throw new IllegalStateException("Condition handler is required for pattern: " + patterns.get(0));
        return handler;
    }

    public @NotNull PatternMeta buildMeta() {
        return new PatternMeta(by, description, List.copyOf(examples), since, category, deprecated);
    }

    public void validate() {
        if (patterns.isEmpty()) {
            throw new IllegalStateException("Condition builder requires at least one pattern");
        }
        if (handler == null) {
            throw new IllegalStateException("Condition builder requires a handler");
        }
        if (by == null) {
            throw new IllegalStateException("Condition builder requires a 'by' (addon name)");
        }
    }
}
