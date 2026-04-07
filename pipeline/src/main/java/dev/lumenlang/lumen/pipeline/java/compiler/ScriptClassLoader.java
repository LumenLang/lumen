package dev.lumenlang.lumen.pipeline.java.compiler;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class loader for dynamically compiled script classes. Allows defining classes from bytecode at runtime.
 *
 * <p>When a class cannot be resolved through the normal parent chain, this loader
 * falls back to any registered extra class loaders (e.g. addon class loaders).
 */
public final class ScriptClassLoader extends ClassLoader {

    private static final List<ClassLoader> EXTRA_LOADERS = new CopyOnWriteArrayList<>();

    public ScriptClassLoader(ClassLoader parent) {
        super(parent);
    }

    /**
     * Registers an additional class loader to try when the parent chain fails.
     *
     * @param loader the extra class loader
     */
    public static void addExtraLoader(@NotNull ClassLoader loader) {
        if (!EXTRA_LOADERS.contains(loader)) EXTRA_LOADERS.add(loader);
    }

    /**
     * Removes a previously registered extra class loader.
     *
     * @param loader the class loader to remove
     */
    public static void removeExtraLoader(@NotNull ClassLoader loader) {
        EXTRA_LOADERS.remove(loader);
    }

    @Override
    protected Class<?> findClass(@NotNull String name) throws ClassNotFoundException {
        for (ClassLoader extra : EXTRA_LOADERS) {
            try {
                return extra.loadClass(name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new ClassNotFoundException(name);
    }

    @SuppressWarnings("UnusedReturnValue")
    public Class<?> define(@NotNull String name, byte[] bytes) {
        return defineClass(name, bytes, 0, bytes.length);
    }
}
