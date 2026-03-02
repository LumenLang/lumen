package net.vansencool.lumen.pipeline.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a Lumen script preload handler.
 *
 * <p>Methods annotated with {@code @LumenPreload} are called immediately after the script
 * class is instantiated but before events and commands are bound. This is the earliest
 * point at which script code can run.
 *
 * <p>The annotated method must have the signature:
 * <pre>{@code
 * public void methodName()
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LumenPreload {
}
