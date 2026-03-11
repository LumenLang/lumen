package net.vansencool.lumen.pipeline.data;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Compile-time schema for a Lumen data class.
 *
 * <p>Holds the type name and an ordered map of field names to their declared types.
 * This is used during code generation to validate field references and produce
 * correctly typed accessor code.
 *
 * @param name   the data type name (e.g. "arena")
 * @param fields ordered map of field name to field type (e.g. "name" to "text", "x1" to "number")
 */
public record DataSchema(@NotNull String name, @NotNull Map<String, FieldType> fields) {

    /**
     * Creates a new DataSchema builder for the given type name.
     *
     * @param name the data type name
     * @return a new builder
     */
    public static @NotNull Builder builder(@NotNull String name) {
        return new Builder(name);
    }

    /**
     * Represents a declared field type in a data class.
     */
    public enum FieldType {
        TEXT("String", "String.valueOf($)"),
        NUMBER("double", "Coerce.toDouble($)"),
        INTEGER("int", "Coerce.toInt($)"),
        BOOLEAN("boolean", "Boolean.parseBoolean(String.valueOf($))"),
        ANY("Object", "$");

        private final @NotNull String javaType;
        private final @NotNull String coercionTemplate;

        FieldType(@NotNull String javaType, @NotNull String coercionTemplate) {
            this.javaType = javaType;
            this.coercionTemplate = coercionTemplate;
        }

        /**
         * Parses a field type name from script source.
         *
         * @param name the type name as written in the data block
         * @return the matching FieldType
         * @throws IllegalArgumentException if the type name is not recognized
         */
        public static @NotNull FieldType fromName(@NotNull String name) {
            return switch (name.toLowerCase()) {
                case "text", "string", "str" -> TEXT;
                case "number", "num", "double", "float", "decimal" -> NUMBER;
                case "integer", "int" -> INTEGER;
                case "boolean", "bool" -> BOOLEAN;
                case "any", "object" -> ANY;
                default -> throw new IllegalArgumentException("Unknown data field type: " + name);
            };
        }

        /**
         * Returns the Java type name for this field type.
         *
         * @return the Java type (e.g. "double", "String")
         */
        public @NotNull String javaType() {
            return javaType;
        }

        /**
         * Returns a Java expression that coerces a raw Object to this type.
         * The {@code $} placeholder is replaced with the actual expression.
         *
         * @param expr the Java expression to coerce
         * @return the coerced Java expression
         */
        public @NotNull String coerce(@NotNull String expr) {
            return coercionTemplate.replace("$", expr);
        }
    }

    /**
     * Builder for constructing DataSchema instances.
     */
    public static final class Builder {
        private final @NotNull String name;
        private final @NotNull Map<String, FieldType> fields = new LinkedHashMap<>();

        private Builder(@NotNull String name) {
            this.name = name;
        }

        /**
         * Adds a field to the schema.
         *
         * @param fieldName the field name
         * @param type      the field type
         * @return this builder
         */
        public @NotNull Builder field(@NotNull String fieldName, @NotNull FieldType type) {
            fields.put(fieldName, type);
            return this;
        }

        /**
         * Builds the DataSchema.
         *
         * @return the constructed schema
         */
        public @NotNull DataSchema build() {
            return new DataSchema(name, Map.copyOf(fields));
        }
    }
}
