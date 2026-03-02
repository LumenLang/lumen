package net.vansencool.lumen.api.event;

import net.vansencool.lumen.api.handler.BlockHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Immutable description of an advanced event definition created via
 * {@link AdvancedEventBuilder}.
 *
 * <p>Advanced events provide full control over code generation. They are not tied
 * to a Bukkit event class and can use any trigger mechanism. The {@link #handler()}
 * is responsible for emitting all generated Java code including method signatures,
 * annotations, and body.
 *
 * <p>Optionally, the definition can declare class-level additions (interfaces, fields,
 * imports) and convenience variables that are emitted automatically by the event block
 * handler before the user's script children.
 *
 * @see AdvancedEventBuilder
 * @see EventRegistrar#advanced
 */
public final class AdvancedEventDefinition {

    private final @NotNull String name;
    private final @Nullable String by;
    private final @Nullable String description;
    private final @NotNull List<String> examples;
    private final @Nullable String since;
    private final @Nullable String category;
    private final boolean deprecated;
    private final @NotNull List<String> interfaces;
    private final @NotNull List<String> fields;
    private final @NotNull List<String> imports;
    private final @NotNull Map<String, EventDefinition.VarEntry> vars;
    private final @NotNull BlockHandler handler;

    AdvancedEventDefinition(@NotNull String name,
                            @Nullable String by,
                            @Nullable String description,
                            @NotNull List<String> examples,
                            @Nullable String since,
                            @Nullable String category,
                            boolean deprecated,
                            @NotNull List<String> interfaces,
                            @NotNull List<String> fields,
                            @NotNull List<String> imports,
                            @NotNull Map<String, EventDefinition.VarEntry> vars,
                            @NotNull BlockHandler handler) {
        this.name = name;
        this.by = by;
        this.description = description;
        this.examples = List.copyOf(examples);
        this.since = since;
        this.category = category;
        this.deprecated = deprecated;
        this.interfaces = List.copyOf(interfaces);
        this.fields = List.copyOf(fields);
        this.imports = List.copyOf(imports);
        this.vars = Collections.unmodifiableMap(vars);
        this.handler = handler;
    }

    /**
     * Returns the script-level event name used in {@code on <name>:} blocks.
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
     * Returns whether this event is deprecated.
     *
     * @return {@code true} if deprecated
     */
    public boolean deprecated() {
        return deprecated;
    }

    /**
     * Returns the fully-qualified interface names that the generated script class
     * should implement when this event is used.
     *
     * @return unmodifiable list of interface FQCNs
     */
    public @NotNull List<String> interfaces() {
        return interfaces;
    }

    /**
     * Returns class-level field declarations that should be added to the generated
     * script class when this event is used.
     *
     * @return unmodifiable list of field declaration lines
     */
    public @NotNull List<String> fields() {
        return fields;
    }

    /**
     * Returns additional fully-qualified class names to import when this event is used.
     *
     * @return unmodifiable list of import FQCNs
     */
    public @NotNull List<String> imports() {
        return imports;
    }

    /**
     * Returns the convenience variable entries for this event definition.
     *
     * <p>These are emitted as local declarations inside the handler method body,
     * before the user's script children are processed.
     *
     * @return an unmodifiable map of variable name to entry
     */
    public @NotNull Map<String, EventDefinition.VarEntry> vars() {
        return vars;
    }

    /**
     * Returns the block handler that controls all generated Java code for this event.
     *
     * @return the block handler
     */
    public @NotNull BlockHandler handler() {
        return handler;
    }
}
