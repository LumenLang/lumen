package dev.lumenlang.lumen.api.pattern;

import dev.lumenlang.lumen.api.type.LumenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Describes a variable that a block provides to its child statements.
 *
 * <p>This record carries both a human readable type string for documentation
 * and an optional {@link LumenType} for tooling and type checking.
 *
 * @param name        the variable name accessible in script child statements (e.g. "player")
 * @param type        a human readable type string for documentation (e.g. "Player", "World")
 * @param lumenType   the compile-time type
 * @param metadata    compile-time metadata entries (e.g. "nullable" to true)
 * @param description a human readable description of this variable, or {@code null}
 */
public record BlockVarInfo(
        @NotNull String name,
        @NotNull String type,
        @NotNull LumenType lumenType,
        @NotNull Map<String, Object> metadata,
        @Nullable String description) {

    /**
     * Creates a BlockVarInfo from a {@link LumenType}, deriving the human readable
     * type string from the type's display name.
     *
     * @param name      the variable name
     * @param lumenType the compile-time type
     */
    public BlockVarInfo(@NotNull String name, @NotNull LumenType lumenType) {
        this(name, lumenType.displayName(), lumenType, Map.of(), null);
    }
}
