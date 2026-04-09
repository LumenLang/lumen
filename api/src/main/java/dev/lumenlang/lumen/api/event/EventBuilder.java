package dev.lumenlang.lumen.api.event;

import dev.lumenlang.lumen.api.pattern.Category;
import dev.lumenlang.lumen.api.type.ObjectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fluent builder for creating {@link EventDefinition} instances.
 *
 * <p>Obtain an instance via {@link EventRegistrar#builder(String)}.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * EventDefinition def = api.events().builder("respawn")
 *     .className("org.bukkit.event.player.PlayerRespawnEvent")
 *     .description("Fires when a player respawns.")
 *     .example("on respawn:")
 *     .since("1.0.0")
 *     .category("Player")
 *     .addVar("player", Types.PLAYER, "event.getPlayer()")
 *     .build();
 * }</pre>
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class EventBuilder {

    private final String name;
    private final Map<String, EventDefinition.VarEntry> vars = new LinkedHashMap<>();
    private final List<String> examples = new ArrayList<>();
    private final List<String> imports = new ArrayList<>();
    private @Nullable String by;
    private String className;
    private String lastVarName;
    private @Nullable String description;
    private @Nullable String since;
    private @Nullable String category;
    private boolean cancellable;
    private boolean deprecated;

    /**
     * Creates a builder for an event with the given script-level name.
     *
     * @param name the name used in {@code on <name>:} blocks in scripts
     */
    public EventBuilder(@NotNull String name) {
        this.name = name;
    }

    /**
     * Sets the addon name that registers this event.
     *
     * @param by the addon name (e.g. "Lumen")
     * @return this builder
     */
    public @NotNull EventBuilder by(@NotNull String by) {
        this.by = by;
        return this;
    }

    /**
     * Sets the fully-qualified Bukkit event class name.
     *
     * @param fqcn the fully-qualified class name
     * @return this builder
     */
    public @NotNull EventBuilder className(@NotNull String fqcn) {
        this.className = fqcn;
        return this;
    }

    /**
     * Sets a human-readable description for this event.
     *
     * @param description the description
     * @return this builder
     */
    public @NotNull EventBuilder description(@NotNull String description) {
        this.description = description;
        return this;
    }

    /**
     * Adds a usage example for this event.
     *
     * @param example an example script line
     * @return this builder
     */
    public @NotNull EventBuilder example(@NotNull String example) {
        this.examples.add(example);
        return this;
    }

    /**
     * Sets the version this event was introduced in.
     *
     * @param version the version string
     * @return this builder
     */
    public @NotNull EventBuilder since(@NotNull String version) {
        this.since = version;
        return this;
    }

    /**
     * Sets the documentation category for this event.
     *
     * @param category the category name
     * @return this builder
     */
    public @NotNull EventBuilder category(@NotNull String category) {
        this.category = category;
        return this;
    }

    /**
     * Sets the documentation category for this event using a {@link Category} constant.
     *
     * @param category the category
     * @return this builder
     */
    public @NotNull EventBuilder category(@NotNull Category category) {
        this.category = category.name();
        return this;
    }

    /**
     * Marks this event as cancellable, meaning the underlying Bukkit event
     * implements {@code Cancellable} and can be cancelled via {@code cancel event}.
     *
     * @param cancellable whether the event is cancellable
     * @return this builder
     */
    public @NotNull EventBuilder cancellable(boolean cancellable) {
        this.cancellable = cancellable;
        return this;
    }

    /**
     * Marks this event as deprecated.
     *
     * @return this builder
     */
    public @NotNull EventBuilder deprecated() {
        this.deprecated = true;
        return this;
    }

    /**
     * Adds a fully-qualified class name to the import list for the generated script class.
     *
     * @param fqcn the fully-qualified class name to import
     * @return this builder
     */
    public @NotNull EventBuilder addImport(@NotNull String fqcn) {
        this.imports.add(fqcn);
        return this;
    }

    /**
     * Adds a typed variable whose Java type is inferred from the ref type handle.
     *
     * <p>This is the preferred overload for variables with a known ref type.
     *
     * @param name    the variable name accessible in script child statements
     * @param refType the logical type category for type checking
     * @param expr    the initialiser expression (e.g. {@code "event.getPlayer()"})
     * @return this builder
     */
    public @NotNull EventBuilder addVar(@NotNull String name, @NotNull ObjectType refType, @NotNull String expr) {
        vars.put(name, new EventDefinition.VarEntry(refType.id(), refType.javaType(), expr));
        this.lastVarName = name;
        return this;
    }

    /**
     * Adds a plain variable (no ref type) to this event definition.
     *
     * <p>Use this for primitives, strings.
     *
     * @param name     the variable name accessible in script child statements
     * @param javaType the Java type name (e.g. {@code Types.BOOLEAN} or a fully qualified class name)
     * @param expr     the initialiser expression (e.g. {@code "event.isSneaking()"})
     * @return this builder
     */
    public @NotNull EventBuilder addVar(@NotNull String name,
                                        @NotNull String javaType,
                                        @NotNull String expr) {
        vars.put(name, new EventDefinition.VarEntry(null, javaType, expr));
        this.lastVarName = name;
        return this;
    }

    /**
     * Attaches a compile-time metadata entry to the most recently added variable.
     *
     * <p>Metadata is propagated to the resulting {@code VarHandle} when the event
     * fires. Downstream patterns can inspect metadata to perform parse-time
     * validation (e.g. checking that an entity variable holds a specific mob class).
     *
     * @param key   the metadata key
     * @param value the metadata value
     * @return this builder
     * @throws IllegalStateException if no variable has been added yet
     */
    public @NotNull EventBuilder withMeta(@NotNull String key, @NotNull Object value) {
        if (lastVarName == null) {
            throw new IllegalStateException("withMeta() must be called after addVar()");
        }
        EventDefinition.VarEntry existing = vars.get(lastVarName);
        Map<String, Object> newMeta = new HashMap<>(existing.metadata());
        newMeta.put(key, value);
        vars.put(lastVarName, new EventDefinition.VarEntry(
                existing.refTypeId(), existing.javaType(), existing.expr(),
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
    public @NotNull EventBuilder varDescription(@NotNull String description) {
        if (lastVarName == null) {
            throw new IllegalStateException("varDescription() must be called after addVar()");
        }
        EventDefinition.VarEntry existing = vars.get(lastVarName);
        vars.put(lastVarName, new EventDefinition.VarEntry(
                existing.refTypeId(), existing.javaType(), existing.expr(),
                existing.metadata(), description));
        return this;
    }

    /**
     * Builds and returns the configured {@link EventDefinition}.
     *
     * @return the completed event definition
     * @throws IllegalStateException if className has not been set
     */
    public @NotNull EventDefinition build() {
        if (className == null) {
            throw new IllegalStateException("className must be set before building");
        }
        return new EventDefinition(name, by, className, new LinkedHashMap<>(vars),
                description, List.copyOf(examples), since, category, cancellable, deprecated, List.copyOf(imports));
    }
}
