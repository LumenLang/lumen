package net.vansencool.lumen.api.pattern;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Represents a documentation category for patterns, conditions, expressions, and blocks.
 *
 * <p>Categories are used to group related patterns together for documentation generation.
 * They are created via {@link Categories#createOrGet(String)} and are interned so that
 * the same name always returns the same instance.
 *
 * @see Categories
 */
public final class Category {

    private final @NotNull String name;

    Category(@NotNull String name) {
        this.name = name;
    }

    /**
     * Returns the display name of this category.
     *
     * @return the category name
     */
    public @NotNull String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category other)) return false;
        return name.equalsIgnoreCase(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase());
    }

    @Override
    public @NotNull String toString() {
        return name;
    }
}
