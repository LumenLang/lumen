package dev.lumenlang.lumen.plugin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a Lumen inventory builder.
 *
 * <p>Methods annotated with {@code @LumenInventory} are discovered at runtime and
 * registered in the inventory registry. When another script opens an inventory by name,
 * the registered method is invoked with the target player.
 *
 * <p>The annotated method must have the signature:
 * <pre>{@code
 * public void methodName(Player player)
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LumenInventory {
    String value();
}
