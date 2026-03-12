package net.vansencool.lumen.api.event;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Immutable description of an event definition created via an {@link EventBuilder}.
 *
 * <p>Addons construct instances through the builder obtained from
 * {@link EventRegistrar#builder(String)}, then pass them to
 * {@link EventRegistrar#register(EventDefinition)}.
 *
 * @see EventBuilder
 */
public final class EventDefinition {

    private final String name;
    private final @Nullable String by;
    private final String className;
    private final Map<String, VarEntry> vars;
    private final @Nullable String description;
    private final @NotNull List<String> examples;
    private final @Nullable String since;
    private final @Nullable String category;
    private final boolean cancellable;
    private final boolean deprecated;

    EventDefinition(@NotNull String name,
                    @Nullable String by,
                    @NotNull String className,
                    @NotNull Map<String, VarEntry> vars,
                    @Nullable String description,
                    @NotNull List<String> examples,
                    @Nullable String since,
                    @Nullable String category,
                    boolean cancellable,
                    boolean deprecated) {
        this.name = name;
        this.by = by;
        this.className = className;
        this.vars = Collections.unmodifiableMap(vars);
        this.description = description;
        this.examples = List.copyOf(examples);
        this.since = since;
        this.category = category;
        this.cancellable = cancellable;
        this.deprecated = deprecated;
    }

    /**
     * Returns the script-level event name.
     *
     * @return the event name
     */
    public @NotNull String name() {
        return name;
    }

    /**
     * Returns the addon name that registered this event, or {@code null} if not set.
     *
     * @return the addon name
     */
    public @Nullable String by() {
        return by;
    }

    /**
     * Returns the fully-qualified Bukkit event class name.
     *
     * @return the event class name
     */
    public @NotNull String className() {
        return className;
    }

    /**
     * Returns the variable entries for this event definition.
     *
     * @return an unmodifiable map of variable name to entry
     */
    public @NotNull Map<String, VarEntry> vars() {
        return vars;
    }

    /**
     * Returns the human-readable description, or {@code null} if not set.
     *
     * @return the description
     */
    public @Nullable String description() {
        return description;
    }

    /**
     * Returns the list of usage examples.
     *
     * @return the examples (never null, may be empty)
     */
    public @NotNull List<String> examples() {
        return examples;
    }

    /**
     * Returns the version this event was introduced in, or {@code null} if not set.
     *
     * @return the since version
     */
    public @Nullable String since() {
        return since;
    }

    /**
     * Returns the documentation category name, or {@code null} if not set.
     *
     * @return the category
     */
    public @Nullable String category() {
        return category;
    }

    /**
     * Returns whether this event is cancellable.
     *
     * <p>A cancellable event means the underlying Bukkit event implements
     * {@code Cancellable} and can be cancelled via the {@code cancel event} statement.
     *
     * @return {@code true} if the event is cancellable
     */
    public boolean cancellable() {
        return cancellable;
    }

    /**
     * Returns whether this event is deprecated.
     *
     * @return {@code true} if deprecated
     */
    public boolean deprecated() {
        return deprecated;
    }

    /**
     * A variable entry in an event definition.
     *
     * @param refTypeId the ref type id for implicit resolution, or {@code null} for plain variables
     * @param javaType  the Java type name (primitive name or fully qualified class name)
     * @param expr      the initialiser expression
     * @param metadata  compile-time metadata entries propagated to the resulting VarHandle
     */
    public record VarEntry(@Nullable String refTypeId,
                           @NotNull String javaType,
                           @NotNull String expr,
                           @NotNull Map<String, Object> metadata) {

        /**
         * Creates a VarEntry with no metadata.
         *
         * @param refTypeId the ref type id, or {@code null}
         * @param javaType  the Java type name (primitive name or fully qualified class name)
         * @param expr      the initialiser expression
         */
        public VarEntry(@Nullable String refTypeId,
                        @NotNull String javaType,
                        @NotNull String expr) {
            this(refTypeId, javaType, expr, Map.of());
        }
    }
}
