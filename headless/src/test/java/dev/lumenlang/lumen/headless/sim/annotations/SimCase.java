package dev.lumenlang.lumen.headless.sim.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a static method that produces a {@code SimulatorCase} so the runner picks it up.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SimCase {

    /**
     * Optional override for the test display name. When empty, the runner falls back to the case's own name.
     */
    String name() default "";
}
