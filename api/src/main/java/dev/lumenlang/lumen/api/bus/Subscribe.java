package dev.lumenlang.lumen.api.bus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an event subscriber on the Lumen {@link EventBus}.
 *
 * <p>The annotated method must accept exactly one parameter. The parameter
 * may be {@link LumenEvent}, any subclass of it, or any interface. When an
 * interface is used, the subscriber receives all events whose concrete type
 * implements that interface.
 *
 * <pre>{@code
 * @Subscribe(priority = Priority.NORMAL)
 * public void onScriptLoad(@NotNull ScriptLoadEvent event) {
 *     // handle event
 * }
 * }</pre>
 *
 * @see EventBus#register(Object)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {

    /**
     * The priority at which this subscriber is invoked.
     *
     * @return the priority (default {@link Priority#NORMAL})
     */
    Priority priority() default Priority.NORMAL;
}
