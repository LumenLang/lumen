package dev.lumenlang.lumen.api.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility methods for narrowing a {@link LumenType} to a specific subtype.
 */
public final class TypeUtils {

    private TypeUtils() {
    }

    /**
     * Extracts the {@link ObjectType} from a LumenType, unwrapping {@link NullableType} if needed.
     *
     * @param type the type to inspect
     * @return the object type, or {@code null} if the underlying type is not an ObjectType
     */
    public static @Nullable ObjectType asObject(@NotNull LumenType type) {
        LumenType unwrapped = type instanceof NullableType n ? n.inner() : type;
        return unwrapped instanceof ObjectType obj ? obj : null;
    }

    /**
     * Extracts the {@link PrimitiveType} from a LumenType, unwrapping {@link NullableType} if needed.
     *
     * @param type the type to inspect
     * @return the primitive type, or {@code null} if the underlying type is not a PrimitiveType
     */
    public static @Nullable PrimitiveType asPrimitive(@NotNull LumenType type) {
        LumenType unwrapped = type instanceof NullableType n ? n.inner() : type;
        return unwrapped instanceof PrimitiveType p ? p : null;
    }

    /**
     * Extracts the {@link CollectionType} from a LumenType, unwrapping {@link NullableType} if needed.
     *
     * @param type the type to inspect
     * @return the collection type, or {@code null} if the underlying type is not a CollectionType
     */
    public static @Nullable CollectionType asCollection(@NotNull LumenType type) {
        LumenType unwrapped = type instanceof NullableType n ? n.inner() : type;
        return unwrapped instanceof CollectionType ct ? ct : null;
    }

    /**
     * Narrows a LumenType to {@link NullableType}.
     *
     * @param type the type to inspect
     * @return the nullable type, or {@code null} if the type is not a NullableType
     */
    public static @Nullable NullableType asNullable(@NotNull LumenType type) {
        return type instanceof NullableType n ? n : null;
    }
}
