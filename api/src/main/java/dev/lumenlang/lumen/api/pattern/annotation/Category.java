package dev.lumenlang.lumen.api.pattern.annotation;

import dev.lumenlang.lumen.api.pattern.Categories;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Documentation category name. Resolves through
 * {@link Categories#createOrGet(String)} at registration time.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Category {

    String value();
}
