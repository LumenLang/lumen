package dev.lumenlang.lumen.pipeline.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a Lumen script command handler.
 *
 * <p>Methods annotated with {@code @LumenCmd} are discovered at runtime by
 * {@code ScriptBinder} and registered as server commands.
 *
 * <p>The annotated method must have the signature:
 * <pre>{@code
 * public boolean methodName(CommandSender sender, String[] args)
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LumenCmd {
    String name();

    String scriptName();

    String description() default "";

    String[] aliases() default {};

    String permission() default "";

    String namespace() default "lumen";
}
