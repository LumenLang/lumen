package dev.lumenlang.lumen.api.binder;

import org.jetbrains.annotations.NotNull;

/**
 * Binds and unbinds annotated methods on compiled script instances.
 */
public interface ScriptAnnotationBinder {

    /**
     * Scans the script class for relevant annotations and binds each annotated method.
     */
    void bind(@NotNull Object instance, @NotNull Class<?> clazz);

    /**
     * Releases resources previously bound for the instance, with semantics chosen by {@code mode}.
     */
    void unbind(@NotNull Object instance, @NotNull BindingMode mode);
}
