package net.vansencool.lumen.api.type;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * API-facing handle for compile-time type information.
 *
 * <p>Lumen tracks a unified type for every variable and expression at compile time.
 * Primitive types ({@code int}, {@code double}, {@code String}, etc.) and object
 * types (backed by a {@link RefTypeHandle}) are both represented through this
 * interface.
 *
 * <p>Addons can register new reference types via {@link RefTypeRegistrar} so they
 * participate in default variable resolution and type checking.
 *
 * @see RefTypeHandle
 * @see RefTypeRegistrar
 */
public interface TypeHandle {

    /**
     * Returns the short identifier for this type (e.g. {@code "int"}, {@code "PLAYER"}).
     *
     * @return the type identifier
     */
    @NotNull String id();

    /**
     * Returns the fully qualified Java type name.
     *
     * @return the Java type name
     */
    @NotNull String javaType();

    /**
     * Returns whether this type represents a numeric value.
     *
     * @return {@code true} for numeric types
     */
    boolean numeric();

    /**
     * Returns the underlying {@link RefTypeHandle} if this type wraps a reference type,
     * or {@code null} for primitives and other non-reference types.
     *
     * @return the ref type handle, or {@code null}
     */
    @Nullable RefTypeHandle refType();

    /**
     * Returns whether this type may be {@code null} at runtime.
     *
     * @return {@code true} if nullable
     */
    default boolean nullable() {
        return false;
    }

    /**
     * Returns the inner type if this is a nullable wrapper, or {@code this} otherwise.
     *
     * <p>Implementations that represent nullable types should override this to
     * return the wrapped inner type.
     *
     * @return the unwrapped type
     */
    default @NotNull TypeHandle unwrap() {
        return this;
    }

    /**
     * Returns whether this type is backed by an object reference type.
     *
     * @return {@code true} when {@link #refType()} is non-null
     */
    default boolean object() {
        return refType() != null;
    }
}
