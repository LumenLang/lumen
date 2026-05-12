package dev.lumenlang.lumen.api.pattern.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an injectable statement handler. The method's runtime
 * section is injected into the script class verbatim. Pair with one or more
 * {@link Pattern} annotations.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Statement {
}
