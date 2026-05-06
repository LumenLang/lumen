package dev.lumenlang.lumen.pipeline.type;

import dev.lumenlang.lumen.api.type.LumenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The fully resolved form of a parsed type annotation.
 *
 * @param type            the resolved type
 * @param tokensConsumed  the number of tokens consumed from the input
 * @param dataSchemaName  the data schema name when resolved from a data class declaration, or {@code null}
 */
public record ParsedType(@NotNull LumenType type, int tokensConsumed, @Nullable String dataSchemaName) {

    public ParsedType(@NotNull LumenType type, int tokensConsumed) {
        this(type, tokensConsumed, null);
    }
}
