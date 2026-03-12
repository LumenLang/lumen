package dev.lumenlang.lumen.pipeline.java.compiled;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime representation of a Lumen data class instance.
 *
 * <p>Each instance carries its data type name and a map of field values. Fields are
 * accessed by name and can be read, written, or checked for existence.
 *
 * <p>Instances are created by the {@code new <type> with field1 val1 field2 val2} expression
 * in Lumen scripts. This class is used directly in generated code.
 */
@SuppressWarnings("unused")
public record DataInstance(@NotNull String type, @NotNull Map<String, Object> fields) implements Serializable {

    /**
     * Creates a new data instance with the given type name and initial fields.
     *
     * @param type   the data type name (e.g. "arena")
     * @param fields the initial field values (copied into an internal mutable map)
     */
    public DataInstance(@NotNull String type, @NotNull Map<String, Object> fields) {
        this.type = type;
        this.fields = new LinkedHashMap<>(fields);
    }

    /**
     * Creates a new empty data instance with the given type name.
     *
     * @param type the data type name
     */
    public DataInstance(@NotNull String type) {
        this(type, Map.of());
    }

    /**
     * Gets the value of a field by name.
     *
     * @param field the field name
     * @return the field value, or null if not set
     */
    public @Nullable Object get(@NotNull String field) {
        return fields.get(field);
    }

    /**
     * Sets the value of a field.
     *
     * @param field the field name
     * @param value the new value
     */
    public void set(@NotNull String field, @Nullable Object value) {
        fields.put(field, value);
    }

    /**
     * Checks whether a field exists in this instance.
     *
     * @param field the field name
     * @return true if the field exists
     */
    public boolean has(@NotNull String field) {
        return fields.containsKey(field);
    }

    /**
     * Returns an unmodifiable view of all fields.
     *
     * @return all field entries
     */
    @Override
    public @NotNull Map<String, Object> fields() {
        return Collections.unmodifiableMap(fields);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataInstance other)) return false;
        return type.equals(other.type) && fields.equals(other.fields);
    }

    @Override
    public @NotNull String toString() {
        return type + fields;
    }
}
