package net.vansencool.lumen.api.type;

import net.vansencool.lumen.api.codegen.EnvironmentAccess;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable API handle for a compile-time reference type.
 *
 * <p>Instances are obtained from the {@link RefTypeRegistrar}. They describe a logical
 * Bukkit category (e.g. {@code PLAYER}, {@code WORLD}) without exposing internal
 * implementation.
 *
 * @see RefTypeRegistrar
 * @see EnvironmentAccess
 */
public interface RefTypeHandle {

    /**
     * Returns the unique identifier for this type (e.g. {@code "PLAYER"}).
     *
     * @return the type id
     */
    @NotNull String id();

    /**
     * Returns the fully-qualified Java class name for this type.
     *
     * @return the Java class name
     */
    @NotNull String javaType();

    /**
     * Returns a Java expression that produces the unique key string for an entity
     * of this type, given the Java variable expression that holds the entity.
     *
     * <p>For example, a player ref type might return
     * {@code "javaVar.getUniqueId().toString()"}.
     *
     * @param javaVar the Java expression referencing the entity instance
     * @return a Java expression that evaluates to a unique key string
     */
    @NotNull String keyExpression(@NotNull String javaVar);
}
