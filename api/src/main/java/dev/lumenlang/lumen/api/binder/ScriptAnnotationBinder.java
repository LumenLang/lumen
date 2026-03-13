package dev.lumenlang.lumen.api.binder;

import org.jetbrains.annotations.NotNull;

/**
 * Handles binding and unbinding of annotated methods on compiled script instances.
 */
public interface ScriptAnnotationBinder {

    /**
     * Scans the given script class for relevant annotations and binds each
     * annotated method.
     *
     * @param instance the script instance
     * @param clazz    the script class
     */
    void bind(@NotNull Object instance, @NotNull Class<?> clazz);

    /**
     * Unbinds all resources previously bound for the given script instance.
     *
     * @param instance the script instance to unbind
     */
    void unbind(@NotNull Object instance);
}
