package dev.lumenlang.lumen.api.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A wrapper indicating the inner type may be {@code null} at runtime.
 *
 * @param inner the wrapped type
 */
public record NullableType(@NotNull LumenType inner) implements LumenType {

    @Override
    public @NotNull String id() {
        return inner.id();
    }

    @Override
    public @NotNull String javaType() {
        return inner.javaType();
    }

    @Override
    public @NotNull String javaTypeName() {
        if (inner instanceof PrimitiveType p) return p.boxedName();
        return inner.javaTypeName();
    }

    @Override
    public @NotNull String displayName() {
        return "nullable " + inner.displayName();
    }

    @Override
    public boolean numeric() {
        return inner.numeric();
    }

    @Override
    public @NotNull LumenType unwrap() {
        return inner.unwrap();
    }

    @Override
    public boolean nullable() {
        return true;
    }

    @Override
    public @NotNull NullableType wrapAsNullable() {
        return this;
    }

    @Override
    public boolean assignableFrom(@NotNull LumenType source) {
        LumenType src = source.unwrap();
        return inner.unwrap().equals(src) || inner.assignableFrom(source);
    }
}
