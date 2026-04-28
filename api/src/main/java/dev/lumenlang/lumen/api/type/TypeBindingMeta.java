package dev.lumenlang.lumen.api.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Immutable documentation metadata for a registered type binding.
 *
 * <p>Every type binding (such as PLAYER, INT, MATERIAL) can carry a
 * {@code TypeBindingMeta} that describes what the type represents,
 * the Java type it resolves to, and usage examples. This information
 * is used for automatic documentation generation and for surfacing
 * accurate diagnostic messages when a binding rejects input.
 *
 * @param displayName a short noun phrase used in diagnostics (e.g. {@code "a player variable"}, {@code "a valid integer"}). Falls back to a humanized form of the binding id when null.
 * @param description a human-readable description of what this type binding represents
 * @param javaType    the fully qualified Java type this binding resolves to (e.g. {@code "org.bukkit.entity.Player"})
 * @param examples    one or more Lumen pattern examples demonstrating usage of this type
 * @param since       the version in which this type binding was introduced
 * @param deprecated  whether this type binding is deprecated and should be avoided
 */
public record TypeBindingMeta(
        @Nullable String displayName,
        @Nullable String description,
        @Nullable String javaType,
        @NotNull List<String> examples,
        @Nullable String since,
        boolean deprecated
) {

    /**
     * A shared empty meta instance used when no documentation is provided.
     */
    public static final TypeBindingMeta EMPTY = new TypeBindingMeta(null, null, null, List.of(), null, false);

    /**
     * Convenience constructor for type bindings that do not provide a custom display name.
     *
     * @param description the long-form description
     * @param javaType    the Java type this binding resolves to
     * @param examples    one or more Lumen pattern usage examples
     * @param since       the version in which this binding was introduced
     * @param deprecated  whether this binding is deprecated
     */
    public TypeBindingMeta(@Nullable String description, @Nullable String javaType, @NotNull List<String> examples, @Nullable String since, boolean deprecated) {
        this(null, description, javaType, examples, since, deprecated);
    }
}
