package dev.lumenlang.lumen.api.inject.index;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Documentation metadata as it appears in the handlers index.
 */
public record IndexedMeta(@Nullable String description, @NotNull List<String> examples, @Nullable String since, @Nullable String category, boolean deprecated) {
}
