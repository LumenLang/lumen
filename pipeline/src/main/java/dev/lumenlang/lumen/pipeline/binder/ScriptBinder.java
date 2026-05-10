package dev.lumenlang.lumen.pipeline.binder;

import dev.lumenlang.lumen.api.binder.BindingMode;
import dev.lumenlang.lumen.pipeline.addon.ScriptBinderManager;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Runtime binding entry point for compiled script instances.
 */
public final class ScriptBinder {

    private static ScriptBinderManager manager;

    private ScriptBinder() {
    }

    public static void init(@NotNull ScriptBinderManager binderManager) {
        manager = binderManager;
    }

    public static void teardown() {
        manager = null;
    }

    public static void bindAll(@NotNull Object inst, @NotNull Class<?> clazz) {
        if (manager == null) {
            throw new IllegalStateException("ScriptBinder has not been initialized");
        }
        manager.bindAll(inst, clazz);
    }

    public static void unbindAll(@NotNull Object inst, @NotNull BindingMode mode) {
        if (manager == null) return;
        manager.unbindAll(inst, mode);
    }

    /**
     * Invokes every method on {@code clazz} carrying the given annotation.
     */
    public static void invokeMethodWithAnnotation(@NotNull Object instance, @NotNull Class<?> clazz, @NotNull Class<? extends Annotation> annotationType) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(annotationType)) {
                try {
                    method.invoke(instance);
                } catch (Throwable t) {
                    LumenLogger.severe("Failed to invoke @" + annotationType.getSimpleName() + " method " + method.getName() + " on " + clazz.getName(), t);
                }
            }
        }
    }
}
