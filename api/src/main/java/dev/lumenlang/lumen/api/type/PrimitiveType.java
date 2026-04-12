package dev.lumenlang.lumen.api.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Primitive value types that map directly to Java primitives or String.
 */
public enum PrimitiveType implements LumenType {
    INT("int", "int", "Integer", 1, List.of("int", "integer"), new LumenTypeMeta("32-bit signed integer. Holds up to about 2.1 billion. Max digits: 10.", "5", List.of())),
    LONG("long", "long", "Long", 2, List.of("long"), new LumenTypeMeta("64-bit signed integer. Holds up to about 9.22 quintillion. Max digits: 19.", "9999999999", List.of())),
    DOUBLE("double", "double", "Double", 4, List.of("double", "number", "num", "decimal"), new LumenTypeMeta("64-bit floating point decimal. Supports up to 15 to 17 significant decimal digits.", "25.5", List.of())),
    FLOAT("float", "float", "Float", 3, List.of("float"), new LumenTypeMeta("32-bit floating point decimal. Supports up to 6 to 9 significant decimal digits.", "1.5", List.of())),
    BOOLEAN("boolean", "boolean", "Boolean", 0, List.of("boolean", "bool"), new LumenTypeMeta("A true or false value.", "true", List.of())),
    STRING("String", "java.lang.String", "String", 0, List.of("string", "text", "str"), new LumenTypeMeta("A sequence of characters.", "\"hello\"", List.of()));

    private final @NotNull String id;
    private final @NotNull String javaType;
    private final @NotNull String boxedName;
    private final int numericRank;
    private final @NotNull List<String> names;
    private final @NotNull LumenTypeMeta meta;

    PrimitiveType(@NotNull String id, @NotNull String javaType, @NotNull String boxedName, int numericRank, @NotNull List<String> names, @NotNull LumenTypeMeta meta) {
        this.id = id;
        this.javaType = javaType;
        this.boxedName = boxedName;
        this.numericRank = numericRank;
        this.names = names;
        this.meta = meta;
    }

    /**
     * Resolves a primitive type from a Java type name string.
     *
     * @param name the type name (e.g. {@code "int"}, {@code "String"}, {@code "java.lang.String"})
     * @return the matching primitive, or {@code null} if not a primitive
     */
    public static @Nullable PrimitiveType fromJavaType(@NotNull String name) {
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
    public static @Nullable PrimitiveType fromName(@NotNull String name) {
        for (PrimitiveType p : values()) {
            if (p.names.contains(name)) return p;
        }
        return null;
    }

    public int numericRank() {
        return numericRank;
    }

    public @NotNull List<String> names() {
        return names;
    }

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

    @Override
    public @NotNull LumenTypeMeta meta() {
        return meta;
    }
}
