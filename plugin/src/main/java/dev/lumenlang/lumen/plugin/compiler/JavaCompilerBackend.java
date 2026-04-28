package dev.lumenlang.lumen.plugin.compiler;

import dev.lumenlang.lumen.plugin.compiler.system.SourceFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Backend that compiles Java source files to bytecode in memory.
 */
public interface JavaCompilerBackend {

    /**
     * Compiles the given source files and returns a map of class names to bytecode.
     *
     * @param files the source files to compile
     * @return map from fully-qualified class name to bytecode
     */
    @NotNull Map<String, byte[]> compileAll(@NotNull List<SourceFile> files);
}
