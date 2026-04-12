package dev.lumenlang.lumen.api.type;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Constants for Lumen's own built-in object types that are not Minecraft specific.
 *
 * <p>These types represent Lumen ecosystem constructs like data class instances.
 * For parameterized types like lists and maps, use {@link #listOf(LumenType)} and
 * {@link #mapOf(LumenType, LumenType)}.
 */
@SuppressWarnings("unused")
public final class BuiltinLumenTypes {

    public static final @NotNull ObjectType DATA = new ObjectType("DATA", "dev.lumenlang.lumen.pipeline.java.compiled.DataInstance");

    private static final @NotNull ObjectType LIST_RAW = new ObjectType("LIST", "java.util.List", "String.valueOf($)", List.of(), new LumenTypeMeta("An ordered collection of values of a single type.", "list of int", List.of()));

    private static final @NotNull ObjectType MAP_RAW = new ObjectType("MAP", "java.util.Map", "String.valueOf($)", List.of(), new LumenTypeMeta("A key-value mapping from one type to another.", "map of string to int", List.of()));

    private BuiltinLumenTypes() {
    }

    public static @NotNull CollectionType listOf(@NotNull LumenType element) {
        return new CollectionType(LIST_RAW, List.of(element));
    }

    public static @NotNull CollectionType mapOf(@NotNull LumenType key, @NotNull LumenType value) {
        return new CollectionType(MAP_RAW, List.of(key, value));
    }

    public static @NotNull ObjectType listRaw() {
        return LIST_RAW;
    }

    public static @NotNull ObjectType mapRaw() {
        return MAP_RAW;
    }

    /**
     * Registers all built-in Lumen types into the {@link LumenTypeRegistry}.
     * Must be called during plugin initialization.
     */
    public static void registerAll() {
        LumenTypeRegistry.register(DATA);
        LumenTypeRegistry.register(LIST_RAW);
        LumenTypeRegistry.register(MAP_RAW);
    }
}
