package net.vansencool.lumen.pipeline.events.def;

import net.vansencool.lumen.pipeline.codegen.TypeEnv;
import net.vansencool.lumen.pipeline.events.EventDefRegistry;
import net.vansencool.lumen.pipeline.var.VarDef;

import java.util.HashMap;
import java.util.Map;

/**
 * Describes a named Lumen event that scripts can react to with {@code on <name>:}.
 *
 * <p>An {@code EventDef} ties together:
 * <ul>
 *   <li>A human-readable event {@link #name} used in script syntax.</li>
 *   <li>The fully-qualified Bukkit {@link #className} of the event class to listen for.</li>
 *   <li>A map of {@link VarDef} entries that are injected as local variables at the top of the
 *       generated handler method and registered into the
 *       {@link TypeEnv} scope.</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * EventDef.builder("join")
 *     .className("org.bukkit.event.player.PlayerJoinEvent")
 *     .addVar("player", RefType.PLAYER,
 *             "org.bukkit.entity.Player", "event.getPlayer()")
 *     .build();
 * }</pre>
 *
 * @see EventDefRegistry
 */
public final class EventDef {

    /**
     * The named variables injected into the generated handler method body, keyed by variable name.
     */
    public final Map<String, VarDef> vars = new HashMap<>();

    /**
     * The script-level event name used in {@code on <name>:} blocks.
     */
    public String name;

    /**
     * The fully-qualified Bukkit event class name that this definition listens for.
     */
    public String className;
}
