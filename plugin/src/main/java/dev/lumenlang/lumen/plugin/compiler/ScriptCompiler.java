package dev.lumenlang.lumen.plugin.compiler;

import dev.lumenlang.lumen.plugin.compiler.system.SourceFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Entry point for script compilation. Delegates to the configured {@link JavaCompilerBackend}.
 */
public final class ScriptCompiler {

    private static volatile JavaCompilerBackend backend;

    /**
     * Sets the active compiler backend. Must be called before any compilation occurs.
     *
     * @param backend the backend to use
     */
    public static void setBackend(@NotNull JavaCompilerBackend backend) {
        ScriptCompiler.backend = backend;
    }

    /**
     * Returns the active compiler backend.
     *
     * @return the backend
     */
    public static @NotNull JavaCompilerBackend backend() {
        JavaCompilerBackend b = backend;
        if (b == null) throw new IllegalStateException("No compiler backend configured");
        return b;
    }

    /**
     * Compiles the given source files using the active backend.
     *
     * @param files the source files to compile
     * @return map from fully-qualified class name to bytecode
     */
    public static @NotNull Map<String, byte[]> compileAll(@NotNull List<SourceFile> files) {
        return backend().compileAll(files);
    }
}
