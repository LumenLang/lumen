package dev.lumenlang.lumen.headless.sim.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a container of {@link SimCase} methods discovered by
 * {@code AnnotatedCaseRunner}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SimulatorTest {
}
