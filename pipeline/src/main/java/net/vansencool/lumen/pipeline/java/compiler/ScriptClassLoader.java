package net.vansencool.lumen.pipeline.java.compiler;

import org.jetbrains.annotations.NotNull;

/**
 * Class loader for dynamically compiled script classes. Allows defining classes from bytecode at runtime.
 */
public final class ScriptClassLoader extends ClassLoader {
    public ScriptClassLoader(ClassLoader parent) {
        super(parent);
    }

    @SuppressWarnings("UnusedReturnValue")
    public Class<?> define(@NotNull String name, byte[] bytes) {
        return defineClass(name, bytes, 0, bytes.length);
    }
}
