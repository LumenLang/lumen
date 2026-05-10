package dev.lumenlang.lumen.api.pattern.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link Statement} handler so its body is emitted into a separate
 * static method on the generated script class instead of being inlined at
 * the call site. Useful when the handler runs in many places and the JIT
 * benefits from profiling a single method, or if its simply long.
 *
 * <p>Conditions and expressions pick this mode automatically when their
 * body has more than one top-level statement; the annotation is only
 * required for statements.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MethodBased {
}
