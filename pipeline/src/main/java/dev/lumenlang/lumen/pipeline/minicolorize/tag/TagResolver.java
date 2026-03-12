package dev.lumenlang.lumen.pipeline.minicolorize.tag;

import dev.lumenlang.lumen.pipeline.minicolorize.MiniColorize;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves a raw tag name into a {@link Tag} instance, or returns null if the
 * tag name is not recognized by this resolver.
 *
 * <p>Register resolvers on a {@link MiniColorize}
 * instance via the builder to extend the tag system with custom tags.
 *
 * @see Tag
 */
@FunctionalInterface
public interface TagResolver {

    /**
     * Attempts to resolve the given tag name into a Tag.
     *
     * @param tagName the raw tag content between {@code <} and {@code >},
     *                e.g. "yellow", "bold", "click:run_command:/seed"
     * @param negated true if the tag was prefixed with {@code !} (e.g. {@code <!bold>})
     * @return the resolved tag, or null if this resolver does not handle the name
     */
    @Nullable Tag resolve(@NotNull String tagName, boolean negated);
}
