package dev.lumenlang.lumen.plugin.scripts.model.source;

import org.jetbrains.annotations.NotNull;

/**
 * Raw script source paired with its file name.
 *
 * @param name   script file name
 * @param source raw {@code .luma} text
 */
public record ScriptSource(@NotNull String name, @NotNull String source) {
}
