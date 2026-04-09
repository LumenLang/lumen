package dev.lumenlang.lumen.api.type;

import org.jetbrains.annotations.NotNull;

/**
 * The kind of a collection type in the Lumen type system.
 */
public enum CollectionKind {
    LIST("LIST", "java.util.List"),
    MAP("MAP", "java.util.Map");

    private final @NotNull String id;
    private final @NotNull String javaType;

    CollectionKind(@NotNull String id, @NotNull String javaType) {
        this.id = id;
        this.javaType = javaType;
    }

    public @NotNull String id() {
        return id;
    }

    public @NotNull String javaType() {
        return javaType;
    }
}
