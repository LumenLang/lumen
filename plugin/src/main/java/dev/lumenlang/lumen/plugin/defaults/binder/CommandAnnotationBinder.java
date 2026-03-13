package dev.lumenlang.lumen.plugin.defaults.binder;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.binder.ScriptAnnotationBinder;
import dev.lumenlang.lumen.plugin.annotations.LumenCmd;
import dev.lumenlang.lumen.plugin.commands.CommandRegistry;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Binder that scans for {@link LumenCmd} annotations and registers
 * the annotated methods as server commands.
 */
@Registration(order = -100)
@SuppressWarnings("unused")
public final class CommandAnnotationBinder implements ScriptAnnotationBinder {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.binders().register(this);
    }

    @Override
    public void bind(@NotNull Object instance, @NotNull Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            LumenCmd cmd = method.getAnnotation(LumenCmd.class);
            if (cmd == null) continue;
            CommandRegistry.register(
                    cmd.scriptName(),
                    cmd.name(),
                    cmd.description().isEmpty() ? null : cmd.description(),
                    cmd.aliases().length == 0 ? null : Arrays.asList(cmd.aliases()),
                    cmd.permission().isEmpty() ? null : cmd.permission(),
                    cmd.namespace().equals("lumen") ? null : cmd.namespace(),
                    instance,
                    method
            );
        }
    }

    @Override
    public void unbind(@NotNull Object instance) {
    }
}
