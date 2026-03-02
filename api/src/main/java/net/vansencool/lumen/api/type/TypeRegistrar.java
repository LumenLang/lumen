package net.vansencool.lumen.api.type;

import net.vansencool.lumen.api.LumenAPI;
import org.jetbrains.annotations.NotNull;

/**
 * API handle for registering custom type bindings.
 *
 * <p>Type bindings define how pattern placeholders like {@code %name:TYPE%} are parsed and
 * converted to Java code. Addons create {@link AddonTypeBinding} implementations and register
 * them here.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * api.types().register(new AddonTypeBinding() {
 *     public @NotNull String id() { return "ENTITY"; }
 *     // ...
 * });
 * }</pre>
 *
 * @see AddonTypeBinding
 * @see LumenAPI#types()
 */
public interface TypeRegistrar {

    /**
     * Registers a custom type binding.
     *
     * <p>If a binding with the same id already exists it is silently replaced.
     *
     * @param binding the type binding to register
     */
    void register(@NotNull AddonTypeBinding binding);
}
