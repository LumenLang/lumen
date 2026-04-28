package dev.lumenlang.lumen.api.handler;

import dev.lumenlang.lumen.api.codegen.HandlerContext;
import org.jetbrains.annotations.NotNull;

/**
 * Handles a matched condition pattern by returning a Java boolean expression as a string.
 *
 * <h2>Example</h2>
 *
 * <pre>{@code
 * api.patterns().condition("%p:PLAYER% is swimming", ctx -> ctx.requireVarHandle("p").java() + ".isSwimming()");
 * }</pre>
 */
@FunctionalInterface
public interface ConditionHandler {

    /**
     * Generates a Java boolean expression for the matched condition.
     *
     * @param ctx the handler context providing bound parameters and environment
     * @return a valid Java boolean expression string
     */
    @NotNull String handle(@NotNull HandlerContext ctx);
}
