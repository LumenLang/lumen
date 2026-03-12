package dev.lumenlang.lumen.api;

import dev.lumenlang.lumen.api.emit.EmitRegistrar;
import dev.lumenlang.lumen.api.event.EventRegistrar;
import dev.lumenlang.lumen.api.pattern.PatternRegistrar;
import dev.lumenlang.lumen.api.placeholder.PlaceholderRegistrar;
import dev.lumenlang.lumen.api.type.RefTypeRegistrar;
import dev.lumenlang.lumen.api.type.TypeRegistrar;
import org.jetbrains.annotations.NotNull;

/**
 * Top-level API handle for extending Lumen with custom patterns, events, types, and ref types.
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
 *   <li>{@link #refTypes()}  -  register new compile-time reference types</li>
 *   <li>{@link #placeholders()}  -  register placeholder properties for ref types</li>
 *   <li>{@link #emitters()}  -  register custom emit handlers (statement forms, block forms, hooks)</li>
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
     * Returns the ref type registrar for registering new compile-time reference types.
     *
     * @return the ref type registrar
     */
    @NotNull RefTypeRegistrar refTypes();

    /**
     * Returns the placeholder registrar for registering placeholder properties on ref types.
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
}
