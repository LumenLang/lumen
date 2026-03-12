package dev.lumenlang.lumen.pipeline.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a Lumen script event handler.
 *
 * <p>Methods annotated with {@code @LumenEvent} are discovered at runtime by
 * {@code ScriptBinder} and bound to Bukkit's event system.
 *
 * <p>The annotated method must have the signature:
 * <pre>{@code
 * public void methodName(SomeEvent event)
 * }</pre>
 *
 * <p>The event class is determined from the annotations's {@link #value()} field.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LumenEvent {
    Class<?> value();
}
