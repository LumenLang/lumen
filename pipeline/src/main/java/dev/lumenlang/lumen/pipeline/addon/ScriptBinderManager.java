package dev.lumenlang.lumen.pipeline.addon;

import dev.lumenlang.lumen.api.binder.BindingMode;
import dev.lumenlang.lumen.api.binder.ScriptAnnotationBinder;
import dev.lumenlang.lumen.api.binder.ScriptBinderRegistrar;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Stores registered binders and dispatches bind/unbind calls to all of them.
 */
public final class ScriptBinderManager implements ScriptBinderRegistrar {

    private final List<ScriptAnnotationBinder> binders = new CopyOnWriteArrayList<>();

    @Override
    public void register(@NotNull ScriptAnnotationBinder binder) {
        binders.add(binder);
    }

    public void bindAll(@NotNull Object instance, @NotNull Class<?> clazz) {
        for (ScriptAnnotationBinder binder : binders) {
            binder.bind(instance, clazz);
        }
    }

    public void unbindAll(@NotNull Object instance, @NotNull BindingMode mode) {
        for (ScriptAnnotationBinder binder : binders) {
            try {
                binder.unbind(instance, mode);
            } catch (Throwable t) {
                LumenLogger.severe("Failed to unbind " + binder.getClass().getSimpleName() + " for " + instance.getClass().getSimpleName(), t);
            }
        }
    }
}
