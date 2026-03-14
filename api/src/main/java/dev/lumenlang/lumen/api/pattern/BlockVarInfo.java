package dev.lumenlang.lumen.api.pattern;

import dev.lumenlang.lumen.api.type.RefTypeHandle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Describes a variable that a block provides to its child statements.
 *
 * <p>This record carries both a human readable type string for documentation
 * and an optional {@link RefTypeHandle} for tooling.
 *
 * @param name        the variable name accessible in script child statements (e.g. "player")
 * @param type        a human readable type string for documentation (e.g. "Player", "World")
 * @param refType     the typed reference handle for tooling, or {@code null} if untyped
 * @param metadata    compile-time metadata entries (e.g. "nullable" to true)
 * @param description a human readable description of this variable, or {@code null}
 */
public record BlockVarInfo(
        @NotNull String name,
        @NotNull String type,
        @Nullable RefTypeHandle refType,
        @NotNull Map<String, Object> metadata,
        @Nullable String description) {

    /**
     * Creates a BlockVarInfo with no ref type, no metadata, and no description.
     *
     * @param name the variable name
     * @param type the human readable type string
     */
    public BlockVarInfo(@NotNull String name, @NotNull String type) {
        this(name, type, null, Map.of(), null);
    }

    /**
     * Creates a BlockVarInfo with no ref type.
     *
     * @param name        the variable name
     * @param type        the human readable type string
     * @param metadata    compile-time metadata entries
     * @param description a human readable description, or {@code null}
     */
    public BlockVarInfo(@NotNull String name, @NotNull String type,
                        @NotNull Map<String, Object> metadata, @Nullable String description) {
        this(name, type, null, metadata, description);
    }

    /**
     * Creates a BlockVarInfo from a {@link RefTypeHandle}, deriving the human readable
     * type string from the Java class simple name.
     *
     * @param name    the variable name
     * @param refType the typed reference handle
     */
    public BlockVarInfo(@NotNull String name, @NotNull RefTypeHandle refType) {
        this(name, simpleNameOf(refType.javaType()), refType, Map.of(), null);
    }

    private static @NotNull String simpleNameOf(@NotNull String fqcn) {
        int dot = fqcn.lastIndexOf('.');
        return dot >= 0 ? fqcn.substring(dot + 1) : fqcn;
    }
}
