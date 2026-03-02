package net.vansencool.lumen.pipeline.language.pattern;

import net.vansencool.lumen.pipeline.language.TypeBinding;
import net.vansencool.lumen.pipeline.typebinding.TypeRegistry;
import org.jetbrains.annotations.NotNull;

/**
 * Metadata for a placeholders within a compiled {@link Pattern}.
 *
 * @param name   the logical name used to look up the matched value in handlers
 * @param typeId the type binding identifier resolved via {@link TypeRegistry#get(String)}
 * @see PatternPart
 * @see TypeBinding
 */
public record Placeholder(@NotNull String name, @NotNull String typeId) {
}