package dev.lumenlang.lumen.pipeline.binder;

import dev.lumenlang.lumen.api.binder.ScriptAnnotationBinder;
import dev.lumenlang.lumen.pipeline.addon.ScriptBinderManager;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Handles runtime binding of compiled script instances by delegating to all
 * registered {@link ScriptAnnotationBinder}
 * implementations.
 */
public final class ScriptBinder {

    private static ScriptBinderManager manager;

    private ScriptBinder() {
    }

    /**
     * Sets the binder manager used for binding script instances.
     *
     * @param binderManager the binder manager
     */
    public static void init(@NotNull ScriptBinderManager binderManager) {
        manager = binderManager;
    }

    /**
     * Clears the binder manager reference.
     */
    public static void teardown() {
        manager = null;
    }

    /**
     * Delegates to all registered binders to bind annotated methods on the
     * given script instance.
     *
     * @param inst  the script instance
     * @param clazz the script class
     */
    public static void bindAll(@NotNull Object inst, @NotNull Class<?> clazz) {
        if (manager == null) {
            throw new IllegalStateException("ScriptBinder has not been initialized");
        }
        manager.bindAll(inst, clazz);
    }

    /**
     * Delegates to all registered binders to unbind the given script instance.
     *
     * @param inst the script instance to unbind
     */
    public static void unbindAll(@NotNull Object inst) {
        if (manager == null) return;
        manager.unbindAll(inst);
    }

    /**
     * Invokes all methods annotated with the specified annotation type on the
     * given class instance.
     *
     * @param instance       the script instance
     * @param clazz          the script class
     * @param annotationType the annotation type to look for
     */
    public static void invokeMethodWithAnnotation(@NotNull Object instance,
                                                  @NotNull Class<?> clazz,
                                                  @NotNull Class<? extends Annotation> annotationType) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(annotationType)) {
                try {
                    method.invoke(instance);
                } catch (Throwable t) {
                    LumenLogger.severe("Failed to invoke @" + annotationType.getSimpleName()
                            + " method " + method.getName() + " on " + clazz.getName(), t);
                }
            }
        }
    }
}
