package dev.lumenlang.lumen.api.type;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * An object reference type representing a Java class in the Lumen type system.
 *
 * <p>Object types are used for Bukkit types like Player, Entity, Location, ItemStack, etc.
 * Each object type carries a unique identifier, the fully qualified Java class name,
 * an optional key template for storage lookups, a list of supertypes for the
 * subtype hierarchy, and optional documentation metadata.
 *
 * <p>The supertype list enables compile-time entity hierarchy validation:
 * {@code player.assignableFrom(entity)} returns {@code false}, while
 * {@code entity.assignableFrom(player)} returns {@code true}.
 *
 * @param id          the unique type identifier (e.g. {@code "PLAYER"})
 * @param javaType    the fully qualified Java class name
 * @param keyTemplate a template for producing a unique runtime key, where {@code $} is the variable name
 * @param superTypes  the parent types in the hierarchy
 * @param meta        documentation metadata for this type
 */
public record ObjectType(@NotNull String id, @NotNull String javaType, @NotNull String keyTemplate, @NotNull List<LumenType> superTypes, @NotNull LumenTypeMeta meta) implements LumenType {

    /**
     * Creates an object type with a default key template and no supertypes.
     *
     * @param id       the unique type identifier
     * @param javaType the fully qualified Java class name
     */
    public ObjectType(@NotNull String id, @NotNull String javaType) {
        this(id, javaType, "String.valueOf($)", List.of(), LumenTypeMeta.EMPTY);
    }

    /**
     * Creates an object type with a custom key template and no supertypes.
     *
     * @param id          the unique type identifier
     * @param javaType    the fully qualified Java class name
     * @param keyTemplate a template for producing a unique runtime key
     */
    public ObjectType(@NotNull String id, @NotNull String javaType, @NotNull String keyTemplate) {
        this(id, javaType, keyTemplate, List.of(), LumenTypeMeta.EMPTY);
    }

    /**
     * Creates an object type with a custom key template and supertypes.
     *
     * @param id          the unique type identifier
     * @param javaType    the fully qualified Java class name
     * @param keyTemplate a template for producing a unique runtime key
     * @param superTypes  the parent types in the hierarchy
     */
    public ObjectType(@NotNull String id, @NotNull String javaType, @NotNull String keyTemplate, @NotNull List<LumenType> superTypes) {
        this(id, javaType, keyTemplate, superTypes, LumenTypeMeta.EMPTY);
    }

    /**
     * Returns whether this type is the same as, or a subtype of, the given type.
     *
     * @param other the type to check against
     * @return {@code true} if this type is a subtype of {@code other}
     */
    public boolean isSubtypeOf(@NotNull ObjectType other) {
        if (this.id.equals(other.id)) return true;
        if (this.javaType.equals(other.javaType)) return true;
        for (LumenType parent : superTypes) {
            if (parent instanceof ObjectType obj && obj.isSubtypeOf(other)) return true;
        }
        return false;
    }

    /**
     * Returns a Java expression that produces the unique key string for a variable
     * of this type, given the Java variable expression that holds the value.
     *
     * @param javaVar the Java expression referencing the instance
     * @return a Java expression that evaluates to a unique key string
     */
    public @NotNull String keyExpression(@NotNull String javaVar) {
        return keyTemplate.replace("$", javaVar);
    }

    @Override
    public @NotNull String javaTypeName() {
        String fqn = javaType;
        return fqn.substring(fqn.lastIndexOf('.') + 1);
    }

    @Override
    public @NotNull String displayName() {
        return id.toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean assignableFrom(@NotNull LumenType source) {
        if (source instanceof NullableType) return false;
        LumenType src = source.unwrap();
        if (this.equals(src)) return true;
        if (src instanceof ObjectType srcObj) return srcObj.isSubtypeOf(this);
        return false;
    }

    @Override
    public @NotNull LumenTypeMeta meta() {
        return meta;
    }
}
