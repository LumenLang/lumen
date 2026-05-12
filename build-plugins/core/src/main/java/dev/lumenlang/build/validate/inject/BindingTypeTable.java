package dev.lumenlang.build.validate.inject;

import dev.lumenlang.build.validate.InjectTypeValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Maps a type-binding id (e.g. {@code "INT"}, {@code "PLAYER"}) to the JVMS
 * field descriptor of the runtime Java type the binding produces (e.g.
 * {@code "I"}, {@code "Lcom/example/Foo;"}).
 *
 * <p>Populated by the build shim and passed into {@link InjectTypeValidator}.
 * Unknown binding ids degrade to a warning, not an error.
 */
public record BindingTypeTable(@NotNull Map<String, String> bindings) {

    public @Nullable String descriptorOf(@NotNull String bindingId) {
        return bindings.get(bindingId);
    }
}
