package dev.lumenlang.lumen.plugin.scripts.runtime;

import dev.lumenlang.lumen.api.annotations.LumenLoad;
import dev.lumenlang.lumen.api.annotations.LumenPreload;
import dev.lumenlang.lumen.pipeline.binder.ScriptBinder;
import dev.lumenlang.lumen.pipeline.java.compiler.ScriptClassLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Builds class loaders for compiled scripts and runs lifecycle hooks.
 */
public final class ScriptActivator {

    private ScriptActivator() {
    }

    public static @NotNull ScriptClassLoader buildLoader(@NotNull Map<String, byte[]> bytecodes) {
        ScriptClassLoader loader = new ScriptClassLoader(ScriptActivator.class.getClassLoader());
        for (var entry : bytecodes.entrySet()) {
            loader.define(entry.getKey(), entry.getValue());
        }
        return loader;
    }

    /**
     * Loads the main class, runs preload, binds events/commands, runs load.
     * If {@code preBuiltLoader} is {@code null}, a fresh loader is built from {@code bytecodes}.
     */
    public static @NotNull LoadedScript activate(@NotNull String scriptName, @NotNull String fqcn, @NotNull Map<String, byte[]> bytecodes, @Nullable ScriptClassLoader preBuiltLoader) {
        ScriptClassLoader loader = preBuiltLoader != null ? preBuiltLoader : buildLoader(bytecodes);
        try {
            Class<?> main = loader.loadClass(fqcn);
            Object inst = main.getDeclaredConstructor().newInstance();
            ScriptBinder.invokeMethodWithAnnotation(inst, main, LumenPreload.class);
            ScriptBinder.bindAll(inst, main);
            ScriptBinder.invokeMethodWithAnnotation(inst, main, LumenLoad.class);
            return new LoadedScript(main, inst);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to load compiled script: " + scriptName, t);
        }
    }
}
