package dev.lumenlang.lumen.plugin.scripts.model.source;

import org.jetbrains.annotations.NotNull;

/**
 * Java source emitted from a parsed Lumen script, ready for compilation.
 *
 * @param scriptName     script file name
 * @param className      generated class name (unqualified)
 * @param javaSource     emitted Java source
 * @param originalSource original {@code .luma} source
 * @param fqcn           fully qualified name of the generated class
 */
public record GeneratedSource(@NotNull String scriptName, @NotNull String className, @NotNull String javaSource,
                              @NotNull String originalSource, @NotNull String fqcn) {
}
