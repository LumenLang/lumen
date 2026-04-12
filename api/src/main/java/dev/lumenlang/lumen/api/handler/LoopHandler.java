package dev.lumenlang.lumen.api.handler;

import dev.lumenlang.lumen.api.codegen.BindingAccess;
import dev.lumenlang.lumen.api.pattern.PatternRegistrar;
import dev.lumenlang.lumen.api.type.LumenType;
import org.jetbrains.annotations.NotNull;

/**
 * Handles a matched loop source pattern by returning the Java iterable expression
 * and the reference type of each element.
 *
 * <p>Loop sources define what collection a {@code loop ... in <source>:} block iterates over.
 * For example, {@code all players} would yield an iterable of Player objects.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * api.patterns().loop(b -> b
 *         .pattern("all players")
 *         .description("Iterates over all online players.")
 *         .example("loop p in all players:")
 *         .handler(ctx -> new LoopResult("Bukkit.getOnlinePlayers()", MinecraftTypes.PLAYER)));
 * }</pre>
 *
 * @see PatternRegistrar#loop
 */
@FunctionalInterface
public interface LoopHandler {

    /**
     * Generates the iterable Java expression and element type for a matched loop source.
     *
     * @param ctx the binding access providing matched parameters
     * @return the loop result containing the iterable expression and element type
     */
    @NotNull LoopResult handle(@NotNull BindingAccess ctx);

    /**
     * Result of a loop source match, containing the Java iterable expression
     * and the type of each yielded element.
     *
     * @param iterableJava a Java expression that evaluates to an {@code Iterable<?>}
     * @param elementType  the compile-time type for each element
     */
    record LoopResult(@NotNull String iterableJava, @NotNull LumenType elementType) {
    }
}
