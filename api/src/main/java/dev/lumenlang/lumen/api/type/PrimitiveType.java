package dev.lumenlang.lumen.api.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Primitive value types that map directly to Java primitives or String.
 */
public enum PrimitiveType implements LumenType {
    INT("int", "int", "Integer", 1, List.of("int", "integer")),
    LONG("long", "long", "Long", 2, List.of("long")),
    DOUBLE("double", "double", "Double", 4, List.of("double", "number", "num", "decimal")),
    FLOAT("float", "float", "Float", 3, List.of("float")),
    BOOLEAN("boolean", "boolean", "Boolean", 0, List.of("boolean", "bool")),
    STRING("String", "java.lang.String", "String", 0, List.of("string", "text", "str"));

    private final @NotNull String id;
    private final @NotNull String javaType;
    private final @NotNull String boxedName;
    private final int numericRank;
    private final @NotNull List<String> names;

    PrimitiveType(@NotNull String id, @NotNull String javaType, @NotNull String boxedName, int numericRank, @NotNull List<String> names) {
        this.id = id;
        this.javaType = javaType;
        this.boxedName = boxedName;
        this.numericRank = numericRank;
        this.names = names;
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
}
