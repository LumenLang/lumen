package dev.lumenlang.lumen.api.annotations;

import dev.lumenlang.lumen.api.LumenAPI;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as the entry point for registration.
 *
 * <p>The annotated method must accept exactly one parameter of type
 * {@link LumenAPI}.
 *
 * <pre>{@code
 * @Registration
 * public class MyRegistrations {
 *     @Call
 *     public void register(@NotNull LumenAPI api) {
 *         api.patterns().statement("heal %who:PLAYER%", ctx ->
 *             ctx.out().line(ctx.java("who") + ".setHealth(20);"));
 *     }
 * }
 * }</pre>
 *
 * @see Registration
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Call {
}
