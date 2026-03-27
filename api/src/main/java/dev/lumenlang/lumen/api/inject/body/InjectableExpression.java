package dev.lumenlang.lumen.api.inject.body;

import java.io.Serializable;

/**
 * A body of Java code that produces a return value, extracted as bytecode and
 * injected directly into the generated script class.
 *
 * <p>Example:
 * <pre>{@code
 * api.patterns().expression(b -> b
 *     .by("MyAddon")
 *     .pattern("location of %who:PLAYER%")
 *     .returnRefTypeId(Types.LOCATION.id())
 *     .injectableHandler(() -> {
 *         Player player = Fakes.fake("who");
 *         return player.getLocation();
 *     })
 * );
 * }</pre>
 */
@FunctionalInterface
public interface InjectableExpression extends Serializable {

    Object body();
}
