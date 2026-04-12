package dev.lumenlang.lumen.api.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Unified compile-time type representation for the Lumen language.
 *
 * <p>Every variable, expression, placeholder, and symbol in a Lumen script is described
 * by a {@code LumenType}. The type system is strict: once a variable is declared with a type,
 * that type cannot change. There is no implicit coercion between incompatible types.
 */
public sealed interface LumenType permits PrimitiveType, ObjectType, CollectionType, NullableType {

    /**
     * Resolves a {@code LumenType} from a type ID string.
     *
     * <p>Checks primitives first, then falls back to the {@link LumenTypeRegistry}.
     *
     * @param id the type identifier (e.g. {@code "PLAYER"}, {@code "int"}, {@code "String"})
     * @return the resolved type, or {@code null}
     */
    static @Nullable LumenType fromId(@NotNull String id) {
        PrimitiveType p = PrimitiveType.fromJavaType(id);
        if (p != null) return p;
        return LumenTypeRegistry.byId(id);
    }

    /**
     * Resolves a {@code LumenType} from a user-facing type name (case-insensitive).
     *
     * <p>Accepts names like {@code "int"}, {@code "string"}, {@code "player"}, {@code "location"}.
     *
     * @param name the type name as written in Lumen source
     * @return the resolved type, or {@code null} if not recognized
     */
    static @Nullable LumenType fromName(@NotNull String name) {
        PrimitiveType p = PrimitiveType.fromName(name.toLowerCase(Locale.ROOT));
        if (p != null) return p;
        return LumenTypeRegistry.byId(name.toUpperCase(Locale.ROOT));
    }

    /**
     * Returns all known type names that can be used in Lumen source.
     *
     * @return a list of all recognized type names
     */
    static @NotNull List<String> allKnownTypeNames() {
        List<String> names = new ArrayList<>();
        for (PrimitiveType p : PrimitiveType.values()) {
            names.addAll(p.names());
        }
        for (ObjectType obj : LumenTypeRegistry.values()) {
            names.add(obj.id().toLowerCase(Locale.ROOT));
        }
        return names;
    }

    /**
     * Resolves a {@code LumenType} from a Java type name string.
     *
     * @param javaType the fully qualified Java type name
     * @return the resolved type, or {@code null}
     */
    static @Nullable LumenType fromJavaType(@NotNull String javaType) {
        PrimitiveType p = PrimitiveType.fromJavaType(javaType);
        if (p != null) return p;
        return LumenTypeRegistry.fromJava(javaType);
    }

    /**
     * Returns the numeric widening result of combining two types in arithmetic.
     *
     * @param a the left operand type
     * @param b the right operand type
     * @return the widened numeric type, or {@code null} if either is non-numeric
     */
    static @Nullable PrimitiveType widenNumeric(@NotNull LumenType a, @NotNull LumenType b) {
        LumenType ua = a.unwrap();
        LumenType ub = b.unwrap();
        if (!ua.numeric() || !ub.numeric()) return null;
        if (ua == PrimitiveType.DOUBLE || ub == PrimitiveType.DOUBLE || ua == PrimitiveType.FLOAT || ub == PrimitiveType.FLOAT) return PrimitiveType.DOUBLE;
        if (ua == PrimitiveType.LONG || ub == PrimitiveType.LONG) return PrimitiveType.LONG;
        return PrimitiveType.INT;
    }

    /**
     * Returns the type identifier used for registry lookups and comparisons.
     *
     * @return the type identifier (e.g. {@code "PLAYER"}, {@code "int"}, {@code "LIST"})
     */
    @NotNull String id();

    /**
     * Returns the fully qualified Java type name for imports and runtime reflection.
     *
     * @return the Java type name (e.g. {@code "org.bukkit.entity.Player"}, {@code "int"})
     */
    @NotNull String javaType();

    /**
     * Returns the Java type name for use in generated code.
     *
     * <p>For parameterized types like {@link CollectionType}, this includes generic
     * arguments (e.g. {@code "List<String>"}).
     *
     * @return the Java type name for code emission
     */
    @NotNull String javaTypeName();

    /**
     * Returns a human-readable type name for error messages and documentation.
     *
     * @return the display name (e.g. {@code "Player"}, {@code "list of String"})
     */
    @NotNull String displayName();

    /**
     * Returns documentation metadata for this type.
     *
     * @return the type's documentation metadata
     */
    default @NotNull LumenTypeMeta meta() {
        return LumenTypeMeta.EMPTY;
    }

    /**
     * Returns whether this type represents a numeric value.
     *
     * @return {@code true} if this type supports arithmetic operations
     */
    default boolean numeric() {
        return false;
    }

    /**
     * Unwraps this type, stripping any {@link NullableType} wrapper.
     *
     * @return the inner type if this is nullable, otherwise {@code this}
     */
    default @NotNull LumenType unwrap() {
        return this;
    }

    /**
     * Returns whether this type is nullable.
     *
     * @return {@code true} if this is a {@link NullableType}
     */
    default boolean nullable() {
        return false;
    }

    /**
     * Wraps this type as a {@link NullableType}.
     *
     * @return a nullable version of this type
     */
    default @NotNull NullableType wrapAsNullable() {
        return new NullableType(this);
    }

    /**
     * Returns whether a value of the given type can be assigned to a variable of this type.
     *
     * @param source the type of the value being assigned
     * @return {@code true} if assignment is type-safe
     */
    default boolean assignableFrom(@NotNull LumenType source) {
        if (source instanceof NullableType && !(this instanceof NullableType)) return false;
        LumenType target = this.unwrap();
        LumenType src = source.unwrap();
        if (target.equals(src)) return true;
        if (target instanceof PrimitiveType tp && src instanceof PrimitiveType sp) {
            if (tp.numeric() && sp.numeric()) return tp.numericRank() >= sp.numericRank();
        }
        return false;
    }
}
