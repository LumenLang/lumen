package dev.lumenlang.lumen.api.inject.index;

import org.jetbrains.annotations.NotNull;

/**
 * One {@code @Inject} parameter as it appears in the handlers index.
 *
 * @param name       effective placeholder name
 * @param binding    type-binding id, e.g. {@code "INT"}
 * @param descriptor JVMS field descriptor of the declared parameter type
 */
public record IndexedParam(@NotNull String name, @NotNull String binding, @NotNull String descriptor) {
}
