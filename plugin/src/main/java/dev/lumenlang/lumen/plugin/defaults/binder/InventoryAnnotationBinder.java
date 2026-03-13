package dev.lumenlang.lumen.plugin.defaults.binder;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.binder.ScriptAnnotationBinder;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.plugin.annotations.LumenInventory;
import dev.lumenlang.lumen.plugin.util.InventoryRegistry;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

/**
 * Binder that scans for {@link LumenInventory} annotations and registers
 * the annotated methods as inventory builders.
 */
@Registration(order = -100)
@SuppressWarnings("unused")
public final class InventoryAnnotationBinder implements ScriptAnnotationBinder {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.binders().register(this);
    }

    @Override
    public void bind(@NotNull Object instance, @NotNull Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            LumenInventory inv = method.getAnnotation(LumenInventory.class);
            if (inv == null) continue;
            try {
                MethodHandle mh = MethodHandles.lookup().findVirtual(
                        clazz, method.getName(),
                        MethodType.methodType(void.class, Player.class));
                InventoryRegistry.register(inv.value(), instance, mh);
            } catch (Throwable t) {
                LumenLogger.severe("Failed to bind inventory builder: " + inv.value(), t);
            }
        }
    }

    @Override
    public void unbind(@NotNull Object instance) {
        InventoryRegistry.clearInstance(instance);
    }
}
