package dev.lumenlang.lumen.plugin.defaults.binder;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.binder.BindingMode;
import dev.lumenlang.lumen.api.binder.ScriptAnnotationBinder;
import dev.lumenlang.lumen.pipeline.persist.GlobalVars;
import org.jetbrains.annotations.NotNull;

/**
 * Drops a script's global vars on teardown.
 */
@Registration(order = -100)
@SuppressWarnings("unused")
public final class GlobalVarsAnnotationBinder implements ScriptAnnotationBinder {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.binders().register(this);
    }

    @Override
    public void bind(@NotNull Object instance, @NotNull Class<?> clazz) {
    }

    @Override
    public void unbind(@NotNull Object instance, @NotNull BindingMode mode) {
        GlobalVars.deleteByPrefix(instance.getClass().getSimpleName() + ".");
    }
}
