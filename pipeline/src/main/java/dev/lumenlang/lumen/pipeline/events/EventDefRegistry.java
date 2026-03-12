package dev.lumenlang.lumen.pipeline.events;

import dev.lumenlang.lumen.api.event.AdvancedEventDefinition;
import dev.lumenlang.lumen.api.event.EventDefinition;
import dev.lumenlang.lumen.pipeline.events.def.EventDef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Global registry for all known {@link EventDef} and {@link AdvancedEventDefinition} instances.
 *
 * <p>Register new events with {@link #register(EventDef)} or
 * {@link #registerAdvanced(AdvancedEventDefinition)} to make {@code on <name>:} syntax
 * available in scripts. All registrations should happen before any scripts are compiled.
 *
 * <p>This class is a static utility and cannot be instantiated.
 *
 * @see EventDef
 * @see AdvancedEventDefinition
 */
@SuppressWarnings("unused")
public class EventDefRegistry {

    private static final Map<String, EventDef> eventDefs = new HashMap<>();
    private static final Map<String, EventDefinition> apiDefinitions = new HashMap<>();
    private static final Map<String, AdvancedEventDefinition> advancedDefs = new HashMap<>();

    private EventDefRegistry() {
    }

    /**
     * Registers an event definition, making its name available to scripts.
     *
     * @param def the event definition to register
     */
    public static void register(@NotNull EventDef def) {
        eventDefs.put(def.name, def);
    }

    /**
     * Stores the original API-level event definition for documentation purposes.
     *
     * @param def the API event definition
     */
    public static void registerApiDefinition(@NotNull EventDefinition def) {
        apiDefinitions.put(def.name(), def);
    }

    /**
     * Returns the {@link EventDef} registered under the given name, or {@code null} if none.
     *
     * <p>Spaces in the name are normalized to underscores so that both
     * {@code "entity_damage"} and {@code "entity damage"} resolve the same event.
     *
     * @param name the script-level event name (e.g. {@code "join"})
     * @return the matching definition, or {@code null}
     */
    public static @Nullable EventDef get(@NotNull String name) {
        EventDef result = eventDefs.get(name);
        if (result == null) {
            result = eventDefs.get(name.replace(' ', '_'));
        }
        return result;
    }

    /**
     * Returns an unmodifiable view of all registered event definitions.
     *
     * @return an unmodifiable map from event name to {@link EventDef}
     */
    public static @NotNull Map<String, EventDef> defs() {
        return Collections.unmodifiableMap(eventDefs);
    }

    /**
     * Returns an unmodifiable view of all registered API-level event definitions.
     *
     * @return an unmodifiable map from event name to {@link EventDefinition}
     */
    public static @NotNull Map<String, EventDefinition> apiDefinitions() {
        return Collections.unmodifiableMap(apiDefinitions);
    }

    /**
     * Returns the mutable internal map of event definitions.
     *
     * <p><strong>Warning:</strong> mutations to the returned map directly affect the registry.
     * Prefer {@link #register(EventDef)} for adding entries.
     *
     * @return the raw internal map
     */
    public static @NotNull Map<String, EventDef> internal() {
        return eventDefs;
    }

    /**
     * Registers an advanced event definition, making its name available to scripts.
     *
     * @param def the advanced event definition to register
     */
    public static void registerAdvanced(@NotNull AdvancedEventDefinition def) {
        advancedDefs.put(def.name(), def);
    }

    /**
     * Returns the {@link AdvancedEventDefinition} registered under the given name, or {@code null}
     * if none.
     *
     * <p>Spaces in the name are normalized to underscores.
     *
     * @param name the script-level event name
     * @return the matching advanced definition, or {@code null}
     */
    public static @Nullable AdvancedEventDefinition getAdvanced(@NotNull String name) {
        AdvancedEventDefinition result = advancedDefs.get(name);
        if (result == null) {
            result = advancedDefs.get(name.replace(' ', '_'));
        }
        return result;
    }

    /**
     * Returns an unmodifiable view of all registered advanced event definitions.
     *
     * @return an unmodifiable map from event name to {@link AdvancedEventDefinition}
     */
    public static @NotNull Map<String, AdvancedEventDefinition> advancedDefs() {
        return Collections.unmodifiableMap(advancedDefs);
    }
}
