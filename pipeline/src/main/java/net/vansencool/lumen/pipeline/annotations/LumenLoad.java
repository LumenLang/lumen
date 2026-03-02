package net.vansencool.lumen.pipeline.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a Lumen script load handler.
 *
 * <p>Methods annotated with {@code @LumenLoad} are called after events and commands have
 * been bound. Use this for initialization logic that depends on the script being fully
 * registered.
 *
 * <p>The annotated method must have the signature:
 * <pre>{@code
 * public void methodName()
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LumenLoad {
}
