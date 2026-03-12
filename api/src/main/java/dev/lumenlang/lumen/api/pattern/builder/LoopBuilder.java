package dev.lumenlang.lumen.api.pattern.builder;

import dev.lumenlang.lumen.api.handler.LoopHandler;
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
 * Fluent builder for registering a loop source pattern with documentation metadata.
 *
 * <p>Loop sources define what collection a {@code loop ... in <source>:} block iterates over.
 *
 * <p>Example usage:
 * <pre>{@code
 * api.patterns().loop(b -> b
 *         .by("Lumen")
 *         .pattern("all players")
 *         .description("Iterates over all online players on the server.")
 *         .example("loop p in all players:")
 *         .since("1.0.0")
 *         .category(Categories.PLAYER)
 *         .handler(ctx -> new LoopResult("Bukkit.getOnlinePlayers()", "PLAYER")));
 * }</pre>
 *
 * @see PatternRegistrar#loop(Consumer)
 */
public final class LoopBuilder {

    private final List<String> patterns = new ArrayList<>();
    private final List<String> examples = new ArrayList<>();
    private @Nullable String by;
    private @Nullable String description;
    private @Nullable String since;
    private @Nullable Category category;
    private boolean deprecated;
    private @Nullable LoopHandler handler;

    /**
     * Sets the addon name that registers this loop source pattern.
     *
     * @param by the addon name (e.g. "Lumen")
     * @return this builder
     */
    public @NotNull LoopBuilder by(@NotNull String by) {
        this.by = by;
        return this;
    }

    /**
     * Adds a pattern string for this loop source.
     *
     * @param pattern the loop source pattern (e.g. {@code "all players"})
     * @return this builder
     */
    public @NotNull LoopBuilder pattern(@NotNull String pattern) {
        this.patterns.add(pattern);
        return this;
    }

    /**
     * Adds multiple pattern strings that all use the same handler.
     *
     * @param patterns the loop source patterns
     * @return this builder
     */
    public @NotNull LoopBuilder patterns(@NotNull String... patterns) {
        Collections.addAll(this.patterns, patterns);
        return this;
    }

    /**
     * Sets the human-readable description of this loop source.
     *
     * @param description the description
     * @return this builder
     */
    public @NotNull LoopBuilder description(@NotNull String description) {
        this.description = description;
        return this;
    }

    /**
     * Adds an example showing how this loop source is used in a Lumen script.
     *
     * @param example a Lumen script example
     * @return this builder
     */
    public @NotNull LoopBuilder example(@NotNull String example) {
        this.examples.add(example);
        return this;
    }

    /**
     * Adds multiple examples showing how this loop source is used in a Lumen script.
     *
     * @param examples the Lumen script examples
     * @return this builder
     */
    public @NotNull LoopBuilder examples(@NotNull String... examples) {
        Collections.addAll(this.examples, examples);
        return this;
    }

    /**
     * Sets the version in which this loop source was introduced.
     *
     * @param since the version string (e.g. "1.0.0")
     * @return this builder
     */
    public @NotNull LoopBuilder since(@NotNull String since) {
        this.since = since;
        return this;
    }

    /**
     * Sets the documentation category for this loop source.
     *
     * @param category the category
     * @return this builder
     */
    public @NotNull LoopBuilder category(@NotNull Category category) {
        this.category = category;
        return this;
    }

    /**
     * Marks this loop source as deprecated.
     *
     * @param deprecated true if this loop source is deprecated
     * @return this builder
     */
    public @NotNull LoopBuilder deprecated(boolean deprecated) {
        this.deprecated = deprecated;
        return this;
    }

    /**
     * Sets the handler that returns the iterable Java expression and element type.
     *
     * @param handler the loop handler
     * @return this builder
     */
    public @NotNull LoopBuilder handler(@NotNull LoopHandler handler) {
        this.handler = handler;
        return this;
    }

    /**
     * @return the registered patterns
     */
    public @NotNull List<String> getPatterns() {
        return patterns;
    }

    /**
     * @return the loop handler
     * @throws IllegalStateException if no handler was set
     */
    public @NotNull LoopHandler getHandler() {
        if (handler == null)
            throw new IllegalStateException("Loop handler is not set for pattern: " + patterns.get(0));
        return handler;
    }

    /**
     * Builds the documentation metadata for this loop source.
     *
     * @return the pattern metadata
     */
    public @NotNull PatternMeta buildMeta() {
        return new PatternMeta(by, description, List.copyOf(examples), since, category, deprecated);
    }

    /**
     * Validates that all required fields are set.
     *
     * @throws IllegalStateException if validation fails
     */
    public void validate() {
        if (patterns.isEmpty()) {
            throw new IllegalStateException("Loop builder requires at least one pattern");
        }
        if (handler == null) {
            throw new IllegalStateException("Loop builder requires a handler");
        }
        if (by == null) {
            throw new IllegalStateException("Loop builder requires a 'by' (addon name)");
        }
    }
}
