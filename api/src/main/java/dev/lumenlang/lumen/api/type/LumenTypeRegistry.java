package dev.lumenlang.lumen.api.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry for all object types in the Lumen type system.
 *
 * <p>Object types (Player, Entity, Location, etc.) are registered here so they can be
 * resolved by id or Java class name during compilation. The registry also stores key
 * templates for producing unique runtime keys from variables of each type.
 *
 * <p>Addons can register custom object types so that their types participate in
 * the compile-time type system.
 */
public final class LumenTypeRegistry {

    private static final Map<String, ObjectType> BY_ID = new ConcurrentHashMap<>();
    private static final Map<String, ObjectType> BY_JAVA = new ConcurrentHashMap<>();

    private LumenTypeRegistry() {
    }

    /**
     * Registers a new object type with a default key template and no supertype.
     *
     * @param id       a unique identifier (e.g. {@code "BLOCK"})
     * @param javaType the fully qualified Java class name
     * @return the registered object type
     */
    public static @NotNull ObjectType register(@NotNull String id, @NotNull String javaType) {
        return register(id, javaType, "String.valueOf($)", null);
    }

    /**
     * Registers a new object type with a custom key template and no supertype.
     *
     * @param id          a unique identifier
     * @param javaType    the fully qualified Java class name
     * @param keyTemplate a template for producing a unique runtime key
     * @return the registered object type
     */
    public static @NotNull ObjectType register(@NotNull String id, @NotNull String javaType, @NotNull String keyTemplate) {
        return register(id, javaType, keyTemplate, null);
    }

    /**
     * Registers a new object type with a supertype relationship.
     *
     * @param id        a unique identifier
     * @param javaType  the fully qualified Java class name
     * @param superType the parent type in the hierarchy, or {@code null} for root types
     * @return the registered object type
     */
    public static @NotNull ObjectType registerWithParent(@NotNull String id, @NotNull String javaType, @Nullable ObjectType superType) {
        return register(id, javaType, "String.valueOf($)", superType);
    }

    /**
     * Registers a new object type with a custom key template and supertype.
     *
     * <p>If the id is already registered with the same Java type, the existing instance is returned.
     *
     * @param id          a unique identifier
     * @param javaType    the fully qualified Java class name
     * @param keyTemplate a template for producing a unique runtime key
     * @param superType   the parent type in the hierarchy, or {@code null}
     * @return the registered object type
     */
    public static @NotNull ObjectType register(@NotNull String id, @NotNull String javaType, @NotNull String keyTemplate, @Nullable ObjectType superType) {
        ObjectType existing = BY_ID.get(id);
        if (existing != null) return existing;
        ObjectType type = new ObjectType(id, javaType, keyTemplate, superType);
        BY_ID.put(id, type);
        BY_JAVA.put(javaType, type);
        return type;
    }

    /**
     * Returns the object type for the given id, or {@code null} if not registered.
     *
     * @param id the type identifier
     * @return the matching object type, or {@code null}
     */
    public static @Nullable ObjectType byId(@NotNull String id) {
        return BY_ID.get(id);
    }

    /**
     * Reverse lookup by fully qualified Java class name.
     *
     * @param javaType the fully qualified class name
     * @return the matching object type, or {@code null}
     */
    public static @Nullable ObjectType fromJava(@NotNull String javaType) {
        return BY_JAVA.get(javaType);
    }

    /**
     * Returns all registered object types.
     *
     * @return an unmodifiable collection of all registered types
     */
    public static @NotNull Collection<ObjectType> values() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }

    /**
     * Returns the key expression for a type and Java variable name.
     *
     * @param type    the object type
     * @param javaVar the Java variable expression
     * @return the key expression
     */
    public static @NotNull String keyExpression(@NotNull ObjectType type, @NotNull String javaVar) {
        return type.keyExpression(javaVar);
    }

    /**
     * Clears all registered types. Used for testing and plugin reload.
     */
    public static void clear() {
        BY_ID.clear();
        BY_JAVA.clear();
    }
}
