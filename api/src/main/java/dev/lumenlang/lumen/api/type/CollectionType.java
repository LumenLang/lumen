package dev.lumenlang.lumen.api.type;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A parameterized type combining a raw object type with generic type arguments.
 * Supports lists, maps, and any other generic container type addons may define.
 *
 * @param rawType       the underlying object type (e.g. LIST, MAP, SET)
 * @param typeArguments the generic type parameters
 */
public record CollectionType(@NotNull ObjectType rawType, @NotNull List<LumenType> typeArguments) implements LumenType {

    @Override
    public @NotNull String id() {
        return rawType.id();
    }

    @Override
    public @NotNull String javaType() {
        return rawType.javaType();
    }

    @Override
    public @NotNull String javaTypeName() {
        if (typeArguments.isEmpty()) return rawType.javaTypeName();
        String args = typeArguments.stream().map(CollectionType::boxedType).collect(Collectors.joining(", "));
        return rawType.javaTypeName() + "<" + args + ">";
    }

    @Override
    public @NotNull String displayName() {
        if (typeArguments.isEmpty()) return rawType.displayName();
        if (typeArguments.size() == 1) return rawType.displayName() + " of " + typeArguments.get(0).displayName();
        if (typeArguments.size() == 2) return rawType.displayName() + " of " + typeArguments.get(0).displayName() + " to " + typeArguments.get(1).displayName();
        String args = typeArguments.stream().map(LumenType::displayName).collect(Collectors.joining(", "));
        return rawType.displayName() + " of " + args;
    }

    @Override
    public boolean assignableFrom(@NotNull LumenType source) {
        if (source instanceof NullableType) return false;
        LumenType src = source.unwrap();
        if (!(src instanceof CollectionType other)) return false;
        if (!rawType.id().equals(other.rawType.id())) return false;
        if (typeArguments.size() != other.typeArguments.size()) return false;
        for (int i = 0; i < typeArguments.size(); i++) {
            if (!typeArguments.get(i).equals(other.typeArguments.get(i))) return false;
        }
        return true;
    }

    private static @NotNull String boxedType(@NotNull LumenType type) {
        if (type instanceof PrimitiveType p) return p.boxedName();
        return type.javaTypeName();
    }
}
