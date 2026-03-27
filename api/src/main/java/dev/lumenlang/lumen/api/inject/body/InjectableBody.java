package dev.lumenlang.lumen.api.inject.body;

import java.io.Serializable;

/**
 * A body of Java code that will be extracted as bytecode and injected directly into
 * the generated script class.
 *
 * <p>Example:
 * <pre>{@code
 * InjectableBody body = () -> {
 *     Player player = Fakes.fake("who");
 *     player.sendMessage("Hello!");
 * };
 * }</pre>
 */
@FunctionalInterface
public interface InjectableBody extends Serializable {

    void body();
}
