package dev.lumenlang.lumen.plugin.defaults.binder;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.binder.BindingMode;
import dev.lumenlang.lumen.api.binder.ScriptAnnotationBinder;
import dev.lumenlang.lumen.plugin.scheduler.ScriptScheduler;
import org.jetbrains.annotations.NotNull;

/**
 * Tears down a script's scheduled tasks when its instance is unbound.
 */
@Registration(order = -100)
@SuppressWarnings("unused")
public final class SchedulerAnnotationBinder implements ScriptAnnotationBinder {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.binders().register(this);
    }

    @Override
    public void bind(@NotNull Object instance, @NotNull Class<?> clazz) {
    }

    @Override
    public void unbind(@NotNull Object instance, @NotNull BindingMode mode) {
        String fqcn = instance.getClass().getName();
        switch (mode) {
            case UNLOAD -> ScriptScheduler.handleUnload(fqcn);
            case RELOAD -> ScriptScheduler.handleReload(fqcn);
        }
    }
}
