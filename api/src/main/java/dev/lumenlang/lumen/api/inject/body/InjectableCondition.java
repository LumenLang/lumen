package dev.lumenlang.lumen.api.inject.body;

import java.io.Serializable;

/**
 * A body of Java code that produces a boolean result, extracted as bytecode and
 * injected directly into the generated script class.
 *
 * <p>Example:
 * <pre>{@code
 * api.patterns().injectableCondition(b -> b
 *     .by("MyAddon")
 *     .pattern("%p:PLAYER% is swimming")
 *     .injectableHandler(() -> {
 *         Player player = Fakes.fake("p");
 *         return player.isSwimming();
 *     })
 * );
 * }</pre>
 */
@FunctionalInterface
public interface InjectableCondition extends Serializable {

    boolean body();
}
