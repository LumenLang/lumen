package dev.lumenlang.lumen.plugin.defaults.binder;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.binder.BindingMode;
import dev.lumenlang.lumen.api.binder.ScriptAnnotationBinder;
import dev.lumenlang.lumen.pipeline.java.compiled.ScriptSourceMap;
import org.jetbrains.annotations.NotNull;

/**
 * Drops a script's generated Java sources from the line-mapping registry on teardown.
 */
@Registration(order = -100)
@SuppressWarnings("unused")
public final class SourceMapAnnotationBinder implements ScriptAnnotationBinder {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.binders().register(this);
    }

    @Override
    public void bind(@NotNull Object instance, @NotNull Class<?> clazz) {
    }

    @Override
    public void unbind(@NotNull Object instance, @NotNull BindingMode mode) {
        ScriptSourceMap.unregisterByClassName(instance.getClass().getSimpleName());
    }
}
