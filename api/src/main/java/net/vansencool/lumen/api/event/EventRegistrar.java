package net.vansencool.lumen.api.event;

import net.vansencool.lumen.api.LumenAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * API handle for registering event definitions that scripts can react to with
 * {@code on <name>:} syntax.
 *
 * <p>Two styles of event are supported:
 * <ol>
 *   <li><b>Regular events</b> (via {@link #register} / {@link #builder}) are tied to a
 *       Bukkit event class and produce a standard {@code @LumenEvent} method.</li>
 *   <li><b>Advanced events</b> (via {@link #advanced}) give the addon developer full control
 *       over the generated code, including method signatures, annotations, interfaces,
 *       and class-level fields. Advanced events are not tied to Bukkit events.</li>
 * </ol>
 *
 * <h2>Regular Example</h2>
 * <pre>{@code
 * api.events().register(
 *     api.events().builder("respawn")
 *         .className("org.bukkit.event.player.PlayerRespawnEvent")
 *         .addVar("player", Types.PLAYER, "event.getPlayer()")
 *         .build()
 * );
 * }</pre>
 *
 * <h2>Advanced Example</h2>
 * <pre>{@code
 * api.events().advanced(b -> b
 *     .name("tick")
 *     .by("Lumen")
 *     .handler(new BlockHandler() { ... })
 * );
 * }</pre>
 *
 * @see EventBuilder
 * @see AdvancedEventBuilder
 * @see LumenAPI#events()
 */
public interface EventRegistrar {

    /**
     * Registers a regular event definition, making its name available to scripts.
     *
     * @param def the event definition to register
     */
    void register(@NotNull EventDefinition def);

    /**
     * Creates a new builder for a regular event definition.
     *
     * @param name the script-level event name (e.g. {@code "respawn"})
     * @return a builder for constructing the event definition
     */
    @NotNull EventBuilder builder(@NotNull String name);

    /**
     * Registers an advanced event definition using a builder consumer.
     *
     * <p>Advanced events give full control over code generation. The handler
     * is responsible for emitting all Java code including method signatures,
     * annotations, and body wrappers.
     *
     * @param builderConsumer consumer that configures the advanced event builder
     */
    void advanced(@NotNull Consumer<AdvancedEventBuilder> builderConsumer);

    /**
     * Registers a pre-built advanced event definition.
     *
     * @param def the advanced event definition to register
     */
    void registerAdvanced(@NotNull AdvancedEventDefinition def);

    /**
     * Looks up a previously registered regular event definition by its script-level name.
     *
     * @param name the event name (e.g. {@code "join"})
     * @return the event definition, or {@code null} if not registered
     */
    @Nullable EventDefinition lookup(@NotNull String name);

    /**
     * Looks up a previously registered advanced event definition by its script-level name.
     *
     * @param name the event name
     * @return the advanced event definition, or {@code null} if not registered
     */
    @Nullable AdvancedEventDefinition lookupAdvanced(@NotNull String name);
}
