package dev.lumenlang.lumen.pipeline.data;

import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Compile-time schema for a Lumen data class.
 *
 * <p>Holds the type name and an ordered map of field names to their declared {@link LumenType}.
 * This is used during code generation to validate field references and produce
 * correctly typed accessor code.
 *
 * @param name   the data type name (e.g. "arena")
 * @param fields ordered map of field name to {@link LumenType}
 */
public record DataSchema(@NotNull String name, @NotNull Map<String, LumenType> fields) {

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
     * Returns a Java expression that casts a raw {@code Object} to the given {@link LumenType}.
     *
     * @param type the target type
     * @param expr the Java expression producing an {@code Object}
     * @return a Java expression with the appropriate cast
     */
    public static @NotNull String castFromObject(@NotNull LumenType type, @NotNull String expr) {
        LumenType unwrapped = type.unwrap();
        if (unwrapped instanceof PrimitiveType p) {
            return switch (p) {
                case INT -> "((Number) " + expr + ").intValue()";
                case LONG -> "((Number) " + expr + ").longValue()";
                case DOUBLE -> "((Number) " + expr + ").doubleValue()";
                case FLOAT -> "((Number) " + expr + ").floatValue()";
                case BOOLEAN -> "(Boolean) " + expr;
                case STRING -> "(String) " + expr;
            };
        }
        return "(" + unwrapped.javaTypeName() + ") " + expr;
    }

    /**
     * Builder for constructing DataSchema instances.
     */
    public static final class Builder {
        private final @NotNull String name;
        private final @NotNull Map<String, LumenType> fields = new LinkedHashMap<>();

        private Builder(@NotNull String name) {
            this.name = name;
        }

        /**
         * Adds a field to the schema.
         *
         * @param fieldName the field name
         * @param type      the field's {@link LumenType}
         * @return this builder
         */
        public @NotNull Builder field(@NotNull String fieldName, @NotNull LumenType type) {
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
