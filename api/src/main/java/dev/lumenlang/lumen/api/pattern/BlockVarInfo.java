package dev.lumenlang.lumen.api.pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Describes a variable that a block provides to its child statements.
 *
 * <p>This record is purely for documentation purposes. Blocks handle their
 * own variable emission in their handlers, but this metadata allows
 * documentation generators to list which variables a block makes available,
 * their types, whether they can be null, and a human readable description.
 *
 * @param name        the variable name accessible in script child statements (e.g. "player")
 * @param type        a human readable type string for documentation (e.g. "Player", "World")
 * @param metadata    compile-time metadata entries (e.g. "nullable" to true)
 * @param description a human readable description of this variable, or {@code null}
 */
public record BlockVarInfo(
        @NotNull String name,
        @NotNull String type,
        @NotNull Map<String, Object> metadata,
        @Nullable String description) {

    /**
     * Creates a BlockVarInfo with no metadata and no description.
     *
     * @param name the variable name
     * @param type the human readable type string
     */
    public BlockVarInfo(@NotNull String name, @NotNull String type) {
        this(name, type, Map.of(), null);
    }
}
