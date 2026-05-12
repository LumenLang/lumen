package dev.lumenlang.lumen.api.pattern.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Pattern string for an injectable handler. Repeatable; multiple patterns
 * share the same handler method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Patterns.class)
public @interface Pattern {

    String value();
}
