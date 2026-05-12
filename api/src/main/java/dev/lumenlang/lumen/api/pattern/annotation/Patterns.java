package dev.lumenlang.lumen.api.pattern.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container for repeated {@link Pattern} annotations. Use {@code @Pattern}
 * directly; the compiler wraps multiple occurrences into this container.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Patterns {

    Pattern[] value();
}
