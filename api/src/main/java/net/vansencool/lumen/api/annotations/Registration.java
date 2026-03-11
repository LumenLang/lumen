package net.vansencool.lumen.api.annotations;

import net.vansencool.lumen.api.LumenAPI;
import net.vansencool.lumen.api.scanner.RegistrationScanner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a registration provider for Lumen's annotations-based scanning system.
 *
 * <p>Classes annotated with {@code @Registration} are discovered by
 * {@link RegistrationScanner} and their {@link Call @Call} methods are invoked
 * with a {@link LumenAPI} instance.
 *
 * <pre>{@code
 * @Registration
 * public class MyRegistrations {
 *     @Call
 *     public void register(@NotNull LumenAPI api) {
 *         api.patterns().statement("heal %who:PLAYER%", (line, ctx, out) ->
 *             out.line(ctx.java("who") + ".setHealth(20);"));
 *     }
 * }
 * }</pre>
 *
 * @see Call
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Registration {

    /**
     * Controls the order in which registration classes are processed.
     *
     * <p>Lower values are processed first. For example, type bindings should use a
     * negative order to ensure they are registered before patterns that reference them.
     *
     * @return the registration order (default {@code 0})
     */
    int order() default 0;
}
