package dev.lumenlang.lumen.api.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A generic collection type with element or key/value type parameters.
 *
 * @param kind        the collection kind
 * @param elementType the element type (for lists) or value type (for maps)
 * @param keyType     the key type (only for maps, {@code null} for lists)
 */
@SuppressWarnings("DataFlowIssue")
public record CollectionType(@NotNull CollectionKind kind, @NotNull LumenType elementType, @Nullable LumenType keyType) implements LumenType {

    @Override
    public @NotNull String id() {
        return kind.id();
    }

    @Override
    public @NotNull String javaType() {
        return kind.javaType();
    }

    @Override
    public @NotNull String javaTypeName() {
        if (kind == CollectionKind.LIST) return "List<" + boxedType(elementType) + ">";
        return "Map<" + boxedType(keyType) + ", " + boxedType(elementType) + ">";
    }

    @Override
    public @NotNull String displayName() {
        if (kind == CollectionKind.LIST) return "list of " + elementType.displayName();
        return "map of " + keyType.displayName() + " to " + elementType.displayName();
    }

    private static @NotNull String boxedType(@NotNull LumenType type) {
        if (type instanceof PrimitiveType p) return p.boxedName();
        return type.javaTypeName();
    }
}
