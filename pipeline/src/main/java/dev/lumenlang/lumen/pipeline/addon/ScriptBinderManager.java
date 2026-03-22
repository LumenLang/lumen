package dev.lumenlang.lumen.pipeline.addon;

import dev.lumenlang.lumen.api.binder.ScriptAnnotationBinder;
import dev.lumenlang.lumen.api.binder.ScriptBinderRegistrar;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Internal implementation of {@link ScriptBinderRegistrar} that stores registered
 * binders and exposes them for use by the script binding system.
 */
public final class ScriptBinderManager implements ScriptBinderRegistrar {

    private final List<ScriptAnnotationBinder> binders = new CopyOnWriteArrayList<>();

    @Override
    public void register(@NotNull ScriptAnnotationBinder binder) {
        binders.add(binder);
    }

    /**
     * Binds all registered binders to the given script instance.
     *
     * @param instance the script instance
     * @param clazz    the script class
     */
    public void bindAll(@NotNull Object instance, @NotNull Class<?> clazz) {
        for (ScriptAnnotationBinder binder : binders) {
            binder.bind(instance, clazz);
        }
    }

    /**
     * Unbinds all registered binders from the given script instance.
     *
     * @param instance the script instance
     */
    public void unbindAll(@NotNull Object instance) {
        for (ScriptAnnotationBinder binder : binders) {
            try {
                binder.unbind(instance);
            } catch (Throwable t) {
                LumenLogger.severe("Failed to unbind " + binder.getClass().getSimpleName()
                        + " for " + instance.getClass().getSimpleName(), t);
            }
        }
    }
}
