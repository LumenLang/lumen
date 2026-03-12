package dev.lumenlang.lumen.api.type;

import dev.lumenlang.lumen.api.LumenAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * API handle for registering and looking up compile-time reference types.
 *
 * <p>Reference types describe the logical Bukkit object category (e.g. {@code PLAYER},
 * {@code WORLD}) that a compile-time variable represents. Addons can register new ref types
 * so that their custom type bindings can participate in default-variable resolution.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * RefTypeHandle block = api.refTypes().register("BLOCK", "org.bukkit.block.Block");
 * }</pre>
 *
 * @see RefTypeHandle
 * @see LumenAPI#refTypes()
 */
public interface RefTypeRegistrar {

    /**
     * Registers a new reference type or returns the existing one if the id is already registered
     * with an identical {@code javaType}. If the id is already registered with a different
     * {@code javaType}, a warning is logged and the existing handle is returned.
     *
     * @param id       a unique identifier (e.g. {@code "BLOCK"})
     * @param javaType the fully-qualified Java class name
     * @return the registered (or existing) handle
     */
    @NotNull RefTypeHandle register(@NotNull String id, @NotNull String javaType);

    /**
     * Returns the handle for the given id, or {@code null} if not registered.
     *
     * @param id the identifier to look up
     * @return the matching handle, or {@code null}
     */
    @Nullable RefTypeHandle byId(@NotNull String id);
}
