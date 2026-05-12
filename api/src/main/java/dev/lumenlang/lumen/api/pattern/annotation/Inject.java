package dev.lumenlang.lumen.api.pattern.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a handler parameter as a script-side placeholder substitution. The
 * parameter name (or {@link #value} when present) must match a placeholder
 * in the handler's pattern. The build plugin verifies the parameter's
 * declared Java type against the placeholder's type binding.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Inject {

    /**
     * Override the placeholder name. Defaults to the parameter's own name.
     */
    String value() default "";
}
