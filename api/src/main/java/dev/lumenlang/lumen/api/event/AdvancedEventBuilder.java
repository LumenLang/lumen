package dev.lumenlang.lumen.api.event;

import dev.lumenlang.lumen.api.handler.BlockHandler;
import dev.lumenlang.lumen.api.pattern.Category;
import dev.lumenlang.lumen.api.type.RefTypeHandle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fluent builder for creating {@link AdvancedEventDefinition} instances.
 *
 * <p>Advanced events give addon developers full control over the generated Java code.
 * Unlike regular events which are always tied to a Bukkit event class with
 * {@code @LumenEvent}, advanced events can:
 * <ul>
 *   <li>Use any trigger mechanism (preload, load, scheduled, custom)</li>
 *   <li>Add interfaces to the generated script class</li>
 *   <li>Add class-level fields</li>
 *   <li>Add custom imports</li>
 *   <li>Generate multiple methods</li>
 *   <li>Use any annotations</li>
 *   <li>Control the full method signature and body</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * api.events().advanced(b -> b
 *     .name("tick")
 *     .by("Lumen")
 *     .description("Fires every server tick.")
 *     .example("on tick:")
 *     .since("1.0.0")
 *     .category("Lifecycle")
 *     .addImport("org.bukkit.scheduler.BukkitRunnable")
 *     .field("private int __tickCount = 0;")
 *     .handler(new BlockHandler() {
 *         public void begin(BindingAccess ctx, JavaOutput out) {
 *             ctx.codegen().addImport("dev.lumenlang.lumen.pipeline.annotations.LumenPreload");
 *             out.line("@LumenPreload");
 *             out.line("public void __tick_" + out.lineNum() + "() {");
 *             out.line("new BukkitRunnable() { public void run() {");
 *         }
 *         public void end(BindingAccess ctx, JavaOutput out) {
 *             out.line("} }.runTaskTimer(Lumen.instance(), 0L, 1L);");
 *             out.line("}");
 *         }
 *     })
 * );
 * }</pre>
 *
 * @see AdvancedEventDefinition
 * @see EventRegistrar#advanced
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class AdvancedEventBuilder {

    private final List<String> examples = new ArrayList<>();
    private final List<String> interfaces = new ArrayList<>();
    private final List<String> fields = new ArrayList<>();
    private final List<String> imports = new ArrayList<>();
    private final Map<String, EventDefinition.VarEntry> vars = new LinkedHashMap<>();
    private @Nullable String name;
    private @Nullable String by;
    private @Nullable String description;
    private @Nullable String since;
    private @Nullable String category;
    private boolean cancellable;
    private boolean deprecated;
    private @Nullable String lastVarName;
    private @Nullable BlockHandler handler;

    /**
     * Creates an empty builder. The name must be set via {@link #name(String)}
     * before calling {@link #build()}.
     */
    public AdvancedEventBuilder() {
    }

    /**
     * Creates a builder for an advanced event with the given script-level name.
     *
     * @param name the name used in {@code on <name>:} blocks in scripts
     */
    public AdvancedEventBuilder(@NotNull String name) {
        this.name = name;
    }

    /**
     * Sets the script-level name for this advanced event.
     *
     * @param name the name used in {@code on <name>:} blocks
     * @return this builder
     */
    public @NotNull AdvancedEventBuilder name(@NotNull String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the addon name that registers this event.
     *
     * @param by the addon name (e.g. "Lumen")
     * @return this builder
     */
    public @NotNull AdvancedEventBuilder by(@NotNull String by) {
        this.by = by;
        return this;
    }

    /**
     * Sets a human-readable description for this event.
     *
     * @param description the description
     * @return this builder
     */
    public @NotNull AdvancedEventBuilder description(@NotNull String description) {
        this.description = description;
        return this;
    }

    /**
     * Adds a usage example for documentation.
     *
     * @param example an example script line
     * @return this builder
     */
    public @NotNull AdvancedEventBuilder example(@NotNull String example) {
        this.examples.add(example);
        return this;
    }

    /**
     * Sets the version this event was introduced in.
     *
     * @param version the version string
     * @return this builder
     */
    public @NotNull AdvancedEventBuilder since(@NotNull String version) {
        this.since = version;
        return this;
    }

    /**
     * Sets the documentation category for this event.
     *
     * @param category the category name
     * @return this builder
     */
    public @NotNull AdvancedEventBuilder category(@NotNull String category) {
        this.category = category;
        return this;
    }

    /**
     * Sets the documentation category for this event using a {@link Category} constant.
     *
     * @param category the category
     * @return this builder
     */
    public @NotNull AdvancedEventBuilder category(@NotNull Category category) {
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
    public @NotNull AdvancedEventBuilder cancellable(boolean cancellable) {
        this.cancellable = cancellable;
        return this;
    }

    /**
     * Marks this event as deprecated.
     *
     * @return this builder
     */
    public @NotNull AdvancedEventBuilder deprecated() {
        this.deprecated = true;
        return this;
    }

    /**
     * Adds a fully-qualified interface name that the generated script class should implement.
     *
     * <p>The interface will also be automatically imported.
     *
     * @param fqcn the fully-qualified interface name (e.g. {@code "java.lang.Runnable"})
     * @return this builder
     */
    public @NotNull AdvancedEventBuilder implement(@NotNull String fqcn) {
        this.interfaces.add(fqcn);
        return this;
    }

    /**
     * Adds a class-level field declaration to the generated script class.
     *
     * @param fieldDeclaration the full field declaration (e.g. {@code "private int counter = 0;"})
     * @return this builder
     */
    public @NotNull AdvancedEventBuilder field(@NotNull String fieldDeclaration) {
        this.fields.add(fieldDeclaration);
        return this;
    }

    /**
     * Adds a fully-qualified class name to the import list for the generated script class.
     *
     * @param fqcn the fully-qualified class name to import
     * @return this builder
     */
    public @NotNull AdvancedEventBuilder addImport(@NotNull String fqcn) {
        this.imports.add(fqcn);
        return this;
    }

    /**
     * Adds a variable that will be available inside the event block.
     *
     * <p>Variables are emitted as local declarations at the top of the handler method
     * body, identical to how regular event variables work.
     *
     * @param name     the variable name accessible in script child statements
     * @param refType  the logical type category for type checking, or {@code null}
     * @param javaType the Java type name (e.g. {@code Types.DOUBLE} or a fully qualified class name)
     * @param expr     the initialiser expression
     * @return this builder
     */
    public @NotNull AdvancedEventBuilder addVar(@NotNull String name,
                                                @Nullable RefTypeHandle refType,
                                                @NotNull String javaType,
                                                @NotNull String expr) {
        String refTypeId = refType != null ? refType.id() : null;
        vars.put(name, new EventDefinition.VarEntry(refTypeId, javaType, expr));
        this.lastVarName = name;
        return this;
    }

    /**
     * Adds a typed variable whose Java type is inferred from the ref type handle.
     *
     * @param name    the variable name
     * @param refType the logical type category
     * @param expr    the initialiser expression
     * @return this builder
     */
    public @NotNull AdvancedEventBuilder addVar(@NotNull String name,
                                                @NotNull RefTypeHandle refType,
                                                @NotNull String expr) {
        vars.put(name, new EventDefinition.VarEntry(refType.id(), refType.javaType(), expr));
        this.lastVarName = name;
        return this;
    }

    /**
     * Adds a plain variable (no ref type).
     *
     * @param name     the variable name
     * @param javaType the Java type name (e.g. {@code Types.BOOLEAN} or a fully qualified class name)
     * @param expr     the initialiser expression
     * @return this builder
     */
    public @NotNull AdvancedEventBuilder addVar(@NotNull String name,
                                                @NotNull String javaType,
                                                @NotNull String expr) {
        vars.put(name, new EventDefinition.VarEntry(null, javaType, expr));
        this.lastVarName = name;
        return this;
    }

    /**
     * Attaches metadata to the most recently added variable.
     *
     * @param key   the metadata key
     * @param value the metadata value
     * @return this builder
     * @throws IllegalStateException if no variable has been added yet
     */
    public @NotNull AdvancedEventBuilder withMeta(@NotNull String key, @NotNull Object value) {
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
    public @NotNull AdvancedEventBuilder varDescription(@NotNull String description) {
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
     * Sets the block handler that controls the generated Java code.
     *
     * <p>The handler's {@code begin()} and {@code end()} methods have full control
     * over the generated code. Unlike regular events, there is no implicit method
     * wrapper or annotation. The handler is responsible for everything: method
     * signatures, annotations, opening and closing braces, etc.
     *
     * @param handler the block handler
     * @return this builder
     */
    public @NotNull AdvancedEventBuilder handler(@NotNull BlockHandler handler) {
        this.handler = handler;
        return this;
    }

    /**
     * Builds and returns the configured {@link AdvancedEventDefinition}.
     *
     * @return the completed advanced event definition
     * @throws IllegalStateException if the handler has not been set
     */
    public @NotNull AdvancedEventDefinition build() {
        if (name == null || name.isEmpty()) {
            throw new IllegalStateException("name must be set before building");
        }
        if (handler == null) {
            throw new IllegalStateException("handler must be set before building");
        }
        return new AdvancedEventDefinition(
                name, by, description, List.copyOf(examples), since, category, cancellable, deprecated,
                List.copyOf(interfaces), List.copyOf(fields), List.copyOf(imports),
                new LinkedHashMap<>(vars), handler);
    }
}
