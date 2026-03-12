package dev.lumenlang.lumen.pipeline.type;

import dev.lumenlang.lumen.api.type.RefTypeHandle;
import dev.lumenlang.lumen.api.type.TypeHandle;
import dev.lumenlang.lumen.pipeline.var.RefType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Unified compile-time type representation for the Lumen language.
 *
 * <p>Every variable, expression, placeholder, and symbol in a Lumen script can be described
 * by a {@code LumenType}.
 *
 * <p>{@code LumenType} replaces the ad-hoc use of {@link RefType} as the sole type metadata
 * carrier. Object reference types wrap their corresponding {@link RefType}, preserving full
 * backward compatibility while extending coverage to primitives and collections.
 *
 * @see Primitive
 * @see ObjectType
 * @see CollectionType
 * @see NullableType
 * @see VoidType
 * @see UnknownType
 */
public sealed interface LumenType extends TypeHandle permits
        LumenType.Primitive,
        LumenType.ObjectType,
        LumenType.CollectionType,
        LumenType.NullableType,
        LumenType.VoidType,
        LumenType.UnknownType {

    /**
     * Resolves a {@code LumenType} from a ref type ID string.
     *
     * <p>Checks primitives first, then falls back to the {@link RefType} registry.
     * Returns {@code null} if the id is not recognized.
     *
     * @param id the type identifier (e.g. {@code "PLAYER"}, {@code "int"}, {@code "String"})
     * @return the resolved type, or {@code null}
     */
    static @Nullable LumenType fromId(@NotNull String id) {
        Primitive p = Primitive.fromJavaType(id);
        if (p != null) return p;

        RefType ref = RefType.byId(id);
        if (ref != null) return new ObjectType(ref);

        return null;
    }

    /**
     * Resolves a {@code LumenType} from a {@link RefTypeHandle}.
     *
     * @param handle the ref type handle
     * @return the corresponding object type
     */
    static @NotNull LumenType fromHandle(@NotNull RefTypeHandle handle) {
        if (handle instanceof RefType rt) {
            return new ObjectType(rt);
        }
        RefType ref = RefType.byId(handle.id());
        if (ref != null) return new ObjectType(ref);
        RefType registered = RefType.register(handle.id(), handle.javaType());
        return new ObjectType(registered);
    }

    /**
     * Resolves a {@code LumenType} from a Java type name string.
     *
     * <p>Checks primitives first, then the {@link RefType} registry by Java type, then by ID.
     *
     * @param javaType the fully qualified Java type name
     * @return the resolved type, or {@code null}
     */
    static @Nullable LumenType fromJavaType(@NotNull String javaType) {
        Primitive p = Primitive.fromJavaType(javaType);
        if (p != null) return p;

        RefType ref = RefType.fromJava(javaType);
        if (ref != null) return new ObjectType(ref);

        return null;
    }

    /**
     * Resolves a {@code LumenType} from a combination of ref type ID and Java type string.
     *
     * <p>Prefers the ref type ID if available; falls back to Java type resolution.
     *
     * @param refTypeId the ref type ID, or {@code null}
     * @param javaType  the Java type name, or {@code null}
     * @return the resolved type, or {@code null} if both are null or unrecognized
     */
    static @Nullable LumenType resolve(@Nullable String refTypeId, @Nullable String javaType) {
        if (refTypeId != null) {
            LumenType t = fromId(refTypeId);
            if (t != null) return t;
        }
        if (javaType != null) {
            return fromJavaType(javaType);
        }
        return null;
    }

    /**
     * Returns the numeric widening result of combining two types in arithmetic.
     *
     * <p>Rules: if either operand is double or float, the result is double.
     * If either is long, the result is long. Otherwise the result is int.
     *
     * @param a the left operand type
     * @param b the right operand type
     * @return the widened numeric type, or {@code null} if either is non-numeric
     */
    static @Nullable Primitive widenNumeric(@NotNull LumenType a, @NotNull LumenType b) {
        LumenType ua = a.unwrap();
        LumenType ub = b.unwrap();
        if (!ua.numeric() || !ub.numeric()) return null;

        if (ua == Primitive.DOUBLE || ub == Primitive.DOUBLE
                || ua == Primitive.FLOAT || ub == Primitive.FLOAT) {
            return Primitive.DOUBLE;
        }
        if (ua == Primitive.LONG || ub == Primitive.LONG) {
            return Primitive.LONG;
        }
        return Primitive.INT;
    }

    /**
     * Returns the short identifier for this type (e.g. {@code "int"}, {@code "PLAYER"}).
     *
     * @return the type identifier
     */
    @NotNull String id();

    /**
     * Returns the fully qualified Java type name for this type.
     *
     * @return the Java type name
     */
    @NotNull String javaType();

    /**
     * Returns whether this type represents a numeric value.
     *
     * @return {@code true} for numeric types
     */
    default boolean numeric() {
        return false;
    }

    /**
     * Returns the underlying {@link RefType} if this type wraps one, or {@code null}.
     *
     * @return the ref type, or {@code null} for non-object types
     */
    default @Nullable RefType refType() {
        return null;
    }

    /**
     * Returns the unwrapped type, stripping any {@link NullableType} wrapper.
     *
     * @return the inner type, or {@code this} if not nullable
     */
    default @NotNull LumenType unwrap() {
        return this;
    }

    /**
     * Wraps this type as nullable.
     *
     * @return a nullable variant of this type
     */
    default @NotNull NullableType wrap() {
        return new NullableType(this);
    }

    /**
     * Primitive value types that map directly to Java primitives or String.
     */
    enum Primitive implements LumenType {
        INT("int", "int"),
        LONG("long", "long"),
        DOUBLE("double", "double"),
        FLOAT("float", "float"),
        BOOLEAN("boolean", "boolean"),
        STRING("String", "java.lang.String");

        private final @NotNull String id;
        private final @NotNull String javaType;

        Primitive(@NotNull String id, @NotNull String javaType) {
            this.id = id;
            this.javaType = javaType;
        }

        /**
         * Resolves a primitive type from a Java type name string.
         *
         * @param name the type name (e.g. {@code "int"}, {@code "String"}, {@code "java.lang.String"})
         * @return the matching primitive, or {@code null} if not a primitive
         */
        public static @Nullable Primitive fromJavaType(@NotNull String name) {
            return switch (name) {
                case "int", "Integer", "java.lang.Integer" -> INT;
                case "long", "Long", "java.lang.Long" -> LONG;
                case "double", "Double", "java.lang.Double" -> DOUBLE;
                case "float", "Float", "java.lang.Float" -> FLOAT;
                case "boolean", "Boolean", "java.lang.Boolean" -> BOOLEAN;
                case "String", "java.lang.String" -> STRING;
                default -> null;
            };
        }

        @Override
        public @NotNull String id() {
            return id;
        }

        @Override
        public @NotNull String javaType() {
            return javaType;
        }

        @Override
        public boolean numeric() {
            return this == INT || this == LONG || this == DOUBLE || this == FLOAT;
        }
    }

    /**
     * The kinds of generic collections.
     */
    enum CollectionKind {
        LIST("LIST", "java.util.List"),
        MAP("MAP", "java.util.Map");

        private final @NotNull String id;
        private final @NotNull String javaType;

        CollectionKind(@NotNull String id, @NotNull String javaType) {
            this.id = id;
            this.javaType = javaType;
        }
    }

    /**
     * An object reference type that wraps a {@link RefType}.
     *
     * <p>Covers Bukkit types like Player, Entity, Location, ItemStack, World, etc.
     *
     * @param ref the underlying ref type
     */
    record ObjectType(@NotNull RefType ref) implements LumenType {

        @Override
        public @NotNull String id() {
            return ref.id;
        }

        @Override
        public @NotNull String javaType() {
            return ref.javaType;
        }

        @Override
        public @NotNull RefType refType() {
            return ref;
        }
    }

    /**
     * A generic collection type with element or key/value type parameters.
     *
     * @param kind        the collection kind
     * @param elementType the element type (for lists) or value type (for maps)
     * @param keyType     the key type (only for maps, {@code null} for lists)
     */
    record CollectionType(
            @NotNull CollectionKind kind,
            @NotNull LumenType elementType,
            @Nullable LumenType keyType
    ) implements LumenType {

        @Override
        public @NotNull String id() {
            return kind.id;
        }

        @Override
        public @NotNull String javaType() {
            return kind.javaType;
        }

        @Override
        public @Nullable RefType refType() {
            return RefType.byId(kind.id);
        }
    }

    /**
     * A wrapper indicating the inner type may be {@code null} at runtime.
     *
     * @param inner the wrapped type
     */
    record NullableType(@NotNull LumenType inner) implements LumenType {

        @Override
        public @NotNull String id() {
            return inner.id();
        }

        @Override
        public @NotNull String javaType() {
            return inner.javaType();
        }

        @Override
        public boolean numeric() {
            return inner.numeric();
        }

        @Override
        public @Nullable RefType refType() {
            return inner.refType();
        }

        @Override
        public @NotNull LumenType unwrap() {
            return inner.unwrap();
        }

        @Override
        public boolean nullable() {
            return true;
        }

        @Override
        public @NotNull NullableType wrap() {
            return this;
        }
    }

    /**
     * Represents the absence of a value.
     *
     * <p>This type is part of the sealed hierarchy but is not currently instantiated
     * anywhere in the pipeline. It exists as a forward declaration for future use
     * when statement return types are tracked at compile time.
     */
    record VoidType() implements LumenType {
        public static final VoidType INSTANCE = new VoidType();

        @Override
        public @NotNull String id() {
            return "void";
        }

        @Override
        public @NotNull String javaType() {
            return "void";
        }
    }

    /**
     * A type that could not be resolved at compile time.
     *
     * <p>This type is part of the sealed hierarchy but is not currently instantiated
     * anywhere in the pipeline. It exists as a forward declaration for future use
     * when type inference needs to represent unresolvable types explicitly.
     *
     * @param hint a human-readable explanation of why the type is unknown, or {@code null}
     */
    record UnknownType(@Nullable String hint) implements LumenType {
        public static final UnknownType INSTANCE = new UnknownType(null);

        @Override
        public @NotNull String id() {
            return "unknown";
        }

        @Override
        public @NotNull String javaType() {
            return "java.lang.Object";
        }
    }
}
