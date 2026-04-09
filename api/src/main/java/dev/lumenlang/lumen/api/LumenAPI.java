package dev.lumenlang.lumen.api;

import dev.lumenlang.lumen.api.binder.ScriptBinderRegistrar;
import dev.lumenlang.lumen.api.emit.EmitRegistrar;
import dev.lumenlang.lumen.api.emit.transform.TransformerRegistrar;
import dev.lumenlang.lumen.api.event.EventRegistrar;
import dev.lumenlang.lumen.api.pattern.PatternRegistrar;
import dev.lumenlang.lumen.api.placeholder.PlaceholderRegistrar;
import dev.lumenlang.lumen.api.type.TypeRegistrar;
import org.jetbrains.annotations.NotNull;

/**
 * Top-level API handle for extending Lumen with custom patterns, events, and types.
 *
 * <p>Addons receive this handle via {@link LumenAddon#onEnable(LumenAPI)}, or plugins can
 * access it via {@link LumenProvider#api()} after Lumen has enabled.
 *
 * <h2>Access Patterns</h2>
 * <pre>{@code
 * // Via LumenAddon (jar-based or plugin-based):
 * public void onEnable(LumenAPI api) {
 *     api.patterns().statement("heal %who:PLAYER%", (line, ctx, out) ->
 *         out.line(ctx.java("who") + ".setHealth(20);")
 *     );
 * }
 *
 * // Directly from a Bukkit plugin that depends on Lumen:
 * LumenAPI api = LumenProvider.api();
 * api.patterns().condition("%p:PLAYER% is swimming", (match, env, ctx) ->
 *     match.ref("p").java() + ".isSwimming()"
 * );
 * }</pre>
 *
 * <p>Through this handle an addon can reach every registrar it needs:
 * <ul>
 *   <li>{@link #patterns()}  -  register statement, block, and condition patterns</li>
 *   <li>{@link #types()}  -  register custom type bindings (e.g. a new {@code ENTITY} type)</li>
 *   <li>{@link #events()}  -  register event definitions for {@code on <name>:} syntax</li>
 *   <li>{@link #placeholders()}  -  register placeholder properties for object types</li>
 *   <li>{@link #emitters()}  -  register custom emit handlers (statement forms, block forms, hooks)</li>
 *   <li>{@link #binders()}  -  register custom script annotation binders</li>
 *   <li>{@link #transformers()}  -  register code transformers (experimental)</li>
 * </ul>
 *
 * <p>Implementations of this interface are provided by Lumen's internal code. Addons should
 * never implement this interface themselves.
 *
 * @see LumenAddon
 */
public interface LumenAPI {

    /**
     * Returns the pattern registrar for registering statement, block, and condition patterns.
     *
     * @return the pattern registrar
     */
    @NotNull PatternRegistrar patterns();

    /**
     * Returns the type registrar for registering custom type bindings.
     *
     * @return the type registrar
     */
    @NotNull TypeRegistrar types();

    /**
     * Returns the event registrar for defining new {@code on <name>:} events.
     *
     * @return the event registrar
     */
    @NotNull EventRegistrar events();

    /**
     * Returns the placeholder registrar for registering placeholder properties on types.
     *
     * @return the placeholder registrar
     */
    @NotNull PlaceholderRegistrar placeholders();

    /**
     * Returns the emit registrar for registering custom statement forms, block forms,
     * and block enter hooks.
     *
     * <p>All built-in language features (variables, config blocks, data blocks, etc.)
     * are registered through this same interface, giving addons the same capabilities
     * as the core language.
     *
     * @return the emit registrar
     */
    @NotNull EmitRegistrar emitters();

    /**
     * Returns the binder registrar for registering custom script annotation binders.
     *
     * <p>Binders registered here are invoked automatically whenever a compiled script
     * class is loaded or unloaded, allowing addons to define their own annotations
     * and binding logic.
     *
     * @return the binder registrar
     */
    @NotNull ScriptBinderRegistrar binders();

    /**
     * Returns the transformer registrar for registering code transformers.
     *
     * <p>Transformers registered here run after code generation to inspect and
     * modify the emitted Java source. Each transformer owns a specific tag and
     * can only modify lines emitted with that tag.
     *
     * <p>This is an experimental feature.
     *
     * @return the transformer registrar
     */
    @NotNull TransformerRegistrar transformers();

    /**
     * Adds an additional path to the script compilation classpath.
     *
     * <p>Addons whose classes are referenced from generated Java code must register
     * their JAR path here so the compiler can resolve them.
     *
     * @param path the absolute file path to add
     */
    void addClasspath(@NotNull String path);

    /**
     * Removes a previously registered extra classpath entry.
     *
     * @param path the absolute file path to remove
     */
    void removeClasspath(@NotNull String path);
}
