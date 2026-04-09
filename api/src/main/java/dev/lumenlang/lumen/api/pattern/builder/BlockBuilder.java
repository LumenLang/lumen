package dev.lumenlang.lumen.api.pattern.builder;

import dev.lumenlang.lumen.api.handler.BlockHandler;
import dev.lumenlang.lumen.api.pattern.BlockVarInfo;
import dev.lumenlang.lumen.api.pattern.Category;
import dev.lumenlang.lumen.api.pattern.PatternMeta;
import dev.lumenlang.lumen.api.pattern.PatternRegistrar;
import dev.lumenlang.lumen.api.type.ObjectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final LinkedHashMap<String, BlockVarInfo> variables = new LinkedHashMap<>();
    private @Nullable String by;
    private @Nullable String description;
    private @Nullable String since;
    private @Nullable Category category;
    private boolean deprecated;
    private boolean supportsRootLevel;
    private boolean supportsBlock = true;
    private @Nullable BlockHandler handler;
    private @Nullable String lastVarName;

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
     * Sets whether this block can be used at the root level of a script.
     *
     * <p>When {@code true}, the block can appear as a top level statement
     * without being nested inside another block. Defaults to {@code false}.
     *
     * @param supportsRootLevel true if this block supports root level usage
     * @return this builder
     */
    public @NotNull BlockBuilder supportsRootLevel(boolean supportsRootLevel) {
        this.supportsRootLevel = supportsRootLevel;
        return this;
    }

    /**
     * Sets whether this block can be used inside another block.
     *
     * <p>When {@code true}, the block can appear nested inside other blocks.
     * Defaults to {@code true}.
     *
     * @param supportsBlock true if this block can be nested inside other blocks
     * @return this builder
     */
    public @NotNull BlockBuilder supportsBlock(boolean supportsBlock) {
        this.supportsBlock = supportsBlock;
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

    /**
     * Adds a variable that this block provides to its child statements.
     *
     * @param name the variable name accessible in script child statements
     * @param type a human readable type string for documentation (e.g. "Player", "World")
     * @return this builder
     */
    public @NotNull BlockBuilder addVar(@NotNull String name, @NotNull String type) {
        variables.put(name, new BlockVarInfo(name, type));
        this.lastVarName = name;
        return this;
    }

    /**
     * Adds a typed variable that this block provides to its child statements.
     *
     * <p>The human readable type string is derived from the ref type's Java class
     * simple name. The {@link RefTypeHandle} is stored so that any tool
     * can resolve the actual compile time type of this variable.
     *
     * @param name    the variable name accessible in script child statements
     * @param refType the typed reference handle (e.g. {@code Types.PLAYER})
     * @return this builder
     */
    public @NotNull BlockBuilder addVar(@NotNull String name, @NotNull ObjectType refType) {
        variables.put(name, new BlockVarInfo(name, refType));
        this.lastVarName = name;
        return this;
    }

    /**
     * Attaches a metadata entry to the most recently added variable.
     *
     * <p>Common metadata keys include {@code "nullable"} (boolean) to indicate
     * that the variable may be {@code null}.
     *
     * @param key   the metadata key
     * @param value the metadata value
     * @return this builder
     * @throws IllegalStateException if no variable has been added yet
     */
    public @NotNull BlockBuilder withMeta(@NotNull String key, @NotNull Object value) {
        if (lastVarName == null) {
            throw new IllegalStateException("withMeta() must be called after addVar()");
        }
        BlockVarInfo existing = variables.get(lastVarName);
        Map<String, Object> newMeta = new HashMap<>(existing.metadata());
        newMeta.put(key, value);
        variables.put(lastVarName, new BlockVarInfo(
                existing.name(), existing.type(), existing.refType(),
                Collections.unmodifiableMap(newMeta), existing.description()));
        return this;
    }

    /**
     * Sets a human readable description on the most recently added variable.
     *
     * <p>This is used by documentation generators to describe what the variable
     * represents for end users.
     *
     * @param description the variable description
     * @return this builder
     * @throws IllegalStateException if no variable has been added yet
     */
    public @NotNull BlockBuilder varDescription(@NotNull String description) {
        if (lastVarName == null) {
            throw new IllegalStateException("varDescription() must be called after addVar()");
        }
        BlockVarInfo existing = variables.get(lastVarName);
        variables.put(lastVarName, new BlockVarInfo(
                existing.name(), existing.type(), existing.refType(),
                existing.metadata(), description));
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

    public @NotNull List<BlockVarInfo> getVariables() {
        return List.copyOf(variables.values());
    }

    public boolean isSupportsRootLevel() {
        return supportsRootLevel;
    }

    public boolean isSupportsBlock() {
        return supportsBlock;
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
