package dev.lumenlang.lumen.pipeline.type;

import dev.lumenlang.lumen.api.type.RefTypeHandle;
import dev.lumenlang.lumen.api.type.TypeHandle;
import dev.lumenlang.lumen.pipeline.var.RefType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Unified compile-time type representation for the Lumen language.
 *
 * <p>Every variable, expression, placeholder, and symbol in a Lumen script is described
 * by a {@code LumenType}. The type system is strict: once a variable is declared with a type,
 * that type cannot change. There is no implicit coercion between incompatible types.
 *
 * <p>Nullability is explicit. Variables are non-null by default and must be declared with
 * {@link NullableType} to hold {@code none}.
 *
 * @see Primitive
 * @see ObjectType
 * @see CollectionType
 * @see NullableType
 * @see VoidType
 */
public sealed interface LumenType extends TypeHandle permits
        LumenType.Primitive,
        LumenType.ObjectType,
        LumenType.CollectionType,
        LumenType.NullableType,
        LumenType.VoidType {

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
     * Resolves a {@code LumenType} from a user-facing type name (case-insensitive).
     *
     * <p>Accepts names like {@code "int"}, {@code "string"}, {@code "player"}, {@code "location"}.
     *
     * @param name the type name as written in Lumen source
     * @return the resolved type, or {@code null} if not recognized
     */
    static @Nullable LumenType fromName(@NotNull String name) {
        Primitive p = Primitive.fromName(name.toLowerCase(Locale.ROOT));
        if (p != null) return p;

        RefType ref = RefType.byId(name.toUpperCase(Locale.ROOT));
        if (ref != null) return new ObjectType(ref);

        return null;
    }

    /**
     * Infers a {@code LumenType} from a Java literal value.
     *
     * @param value the literal value
     * @return the inferred type
     */
    static @NotNull LumenType fromLiteral(@NotNull Object value) {
        if (value instanceof Integer) return Primitive.INT;
        if (value instanceof Long) return Primitive.LONG;
        if (value instanceof Double) return Primitive.DOUBLE;
        if (value instanceof Float) return Primitive.FLOAT;
        if (value instanceof Boolean) return Primitive.BOOLEAN;
        if (value instanceof String) return Primitive.STRING;
        return Primitive.STRING;
    }

    /**
     * Creates a typed list type.
     *
     * @param element the element type
     * @return the collection type for a list
     */
    static @NotNull CollectionType listOf(@NotNull LumenType element) {
        return new CollectionType(CollectionKind.LIST, element, null);
    }

    /**
     * Creates a typed map type.
     *
     * @param key   the key type
     * @param value the value type
     * @return the collection type for a map
     */
    static @NotNull CollectionType mapOf(@NotNull LumenType key, @NotNull LumenType value) {
        return new CollectionType(CollectionKind.MAP, value, key);
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

    @NotNull String id();

    @NotNull String javaType();

    /**
     * Returns the Java type name for use in generated code (e.g. {@code "int"}, {@code "Player"},
     * {@code "List<Integer>"}).
     *
     * <p>For object types, this returns the simple class name (import must be added separately).
     * For nullable primitives, this returns the boxed type name since Java primitives cannot be null.
     *
     * @return the Java type name for code emission
     */
    @NotNull String javaTypeName();

    /**
     * Returns a human-readable type name for error messages (e.g. {@code "int"}, {@code "string"},
     * {@code "nullable player"}, {@code "list of int"}).
     *
     * @return the display name
     */
    @NotNull String displayName();

    default boolean numeric() {
        return false;
    }

    default @Nullable RefType refType() {
        return null;
    }

    default @NotNull LumenType unwrap() {
        return this;
    }

    default @NotNull NullableType wrap() {
        return new NullableType(this);
    }

    /**
     * Returns whether a value of the given type can be assigned to a variable of this type.
     *
     * <p>Assignment rules:
     * <ul>
     *   <li>Same type: always allowed</li>
     *   <li>Non-null into nullable: allowed</li>
     *   <li>Nullable into non-null: forbidden</li>
     *   <li>Lossless numeric widening (int to long, int to double, etc.): allowed</li>
     *   <li>Lossy numeric narrowing (double to int, long to int): forbidden</li>
     *   <li>Different non-numeric types: forbidden</li>
     * </ul>
     *
     * @param source the type of the value being assigned
     * @return {@code true} if assignment is type-safe
     */
    default boolean assignableFrom(@NotNull LumenType source) {
        if (source instanceof NullableType && !(this instanceof NullableType)) {
            return false;
        }

        LumenType target = this.unwrap();
        LumenType src = source.unwrap();

        if (target.equals(src)) return true;

        if (target instanceof Primitive tp && src instanceof Primitive sp) {
            if (tp.numeric() && sp.numeric()) {
                return tp.numericRank() >= sp.numericRank();
            }
        }

        return false;
    }

    /**
     * Primitive value types that map directly to Java primitives or String.
     */
    enum Primitive implements LumenType {
        INT("int", "int", "Integer", 1),
        LONG("long", "long", "Long", 2),
        DOUBLE("double", "double", "Double", 4),
        FLOAT("float", "float", "Float", 3),
        BOOLEAN("boolean", "boolean", "Boolean", 0),
        STRING("String", "java.lang.String", "String", 0);

        private final @NotNull String id;
        private final @NotNull String javaType;
        private final @NotNull String boxedName;
        private final int numericRank;

        Primitive(@NotNull String id, @NotNull String javaType, @NotNull String boxedName, int numericRank) {
            this.id = id;
            this.javaType = javaType;
            this.boxedName = boxedName;
            this.numericRank = numericRank;
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

        /**
         * Resolves a primitive type from a user-facing Lumen type name.
         *
         * @param name the lowercase type name (e.g. {@code "int"}, {@code "string"}, {@code "bool"})
         * @return the matching primitive, or {@code null} if not a primitive
         */
        public static @Nullable Primitive fromName(@NotNull String name) {
            return switch (name) {
                case "int", "integer" -> INT;
                case "long" -> LONG;
                case "double" -> DOUBLE;
                case "float" -> FLOAT;
                case "boolean", "bool" -> BOOLEAN;
                case "string", "text" -> STRING;
                default -> null;
            };
        }

        /**
         * Returns the numeric widening rank for this primitive.
         * Higher rank means wider type: INT(1) < LONG(2) < FLOAT(3) < DOUBLE(4).
         * Non-numeric types return 0.
         *
         * @return the numeric rank
         */
        public int numericRank() {
            return numericRank;
        }

        /**
         * Returns the boxed Java type name (e.g. {@code "Integer"} for {@code INT}).
         *
         * @return the boxed type name
         */
        public @NotNull String boxedName() {
            return boxedName;
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
        public @NotNull String javaTypeName() {
            return id;
        }

        @Override
        public @NotNull String displayName() {
            return id.toLowerCase(Locale.ROOT);
        }

        @Override
        public boolean numeric() {
            return this == INT || this == LONG || this == DOUBLE || this == FLOAT;
        }
    }

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
        public @NotNull String javaTypeName() {
            String fqn = ref.javaType;
            return fqn.substring(fqn.lastIndexOf('.') + 1);
        }

        @Override
        public @NotNull String displayName() {
            return ref.id.toLowerCase(Locale.ROOT);
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
        public @NotNull String javaTypeName() {
            if (kind == CollectionKind.LIST) {
                return "List<" + boxedType(elementType) + ">";
            }
            return "Map<" + boxedType(keyType) + ", " + boxedType(elementType) + ">";
        }

        @Override
        public @NotNull String displayName() {
            if (kind == CollectionKind.LIST) {
                return "list of " + elementType.displayName();
            }
            return "map of " + keyType.displayName() + " to " + elementType.displayName();
        }

        @Override
        public @Nullable RefType refType() {
            return RefType.byId(kind.id);
        }

        private static @NotNull String boxedType(@NotNull LumenType type) {
            if (type instanceof Primitive p) return p.boxedName();
            return type.javaTypeName();
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
        public @NotNull String javaTypeName() {
            if (inner instanceof Primitive p) return p.boxedName();
            return inner.javaTypeName();
        }

        @Override
        public @NotNull String displayName() {
            return "nullable " + inner.displayName();
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

        @Override
        public boolean assignableFrom(@NotNull LumenType source) {
            LumenType src = source.unwrap();
            return inner.unwrap().equals(src) || inner.assignableFrom(source);
        }
    }

    /**
     * Represents the absence of a value (statement return type).
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

        @Override
        public @NotNull String javaTypeName() {
            return "void";
        }

        @Override
        public @NotNull String displayName() {
            return "void";
        }
    }
}
