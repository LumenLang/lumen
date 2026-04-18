package dev.lumenlang.lumen.pipeline.language.pattern;

import dev.lumenlang.lumen.pipeline.typebinding.TypeRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * Metadata for a placeholder within a compiled {@link Pattern}.
 *
 * <p>When a pattern declares {@code %p:NULLABLE_PLAYER%}, the {@code NULLABLE_} prefix
 * is stripped and the placeholder is created with {@code typeId = "PLAYER"} and
 * {@code nullable = true}. The actual type binding name remains unchanged.
 *
 * @param name     the logical name used to look up the matched value in handlers
 * @param typeId   the type binding identifier resolved via {@link TypeRegistry#get(String)}
 * @param nullable whether this placeholder accepts nullable values
 */
public record Placeholder(@NotNull String name, @NotNull String typeId, boolean nullable) {
}