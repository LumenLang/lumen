package dev.lumenlang.lumen.plugin.scripts.model.compiled;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Compiled script bytecode bundle ready to be loaded.
 *
 * @param name      script file name
 * @param fqcn      fully qualified main class name
 * @param bytecodes class name to bytecode bytes
 * @param timings   parse and compile timings
 */
public record PreparedScript(@NotNull String name, @NotNull String fqcn, @NotNull Map<String, byte[]> bytecodes,
                             @NotNull CompileTimings timings) {
}
