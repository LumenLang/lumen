package dev.lumenlang.lumen.headless.sim.base;

import dev.lumenlang.lumen.headless.HeadlessLumen;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;

/**
 * Base class for tests that need the Lumen registration system loaded.
 *
 * <p>Bootstraps {@link HeadlessLumen} once per JVM in a static initializer so subclasses
 * can use {@link PatternRegistry#instance()} and other registry singletons directly.
 */
public abstract class HeadlessTestBase {

    static {
        new HeadlessLumen();
    }
}
