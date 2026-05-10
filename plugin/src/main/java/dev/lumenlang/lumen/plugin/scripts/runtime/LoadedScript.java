package dev.lumenlang.lumen.plugin.scripts.runtime;

import org.jetbrains.annotations.NotNull;

/**
 * Live script: its compiled class and the singleton instance.
 *
 * @param clazz    main script class
 * @param instance running script instance
 */
public record LoadedScript(@NotNull Class<?> clazz, @NotNull Object instance) {
}
