package dev.lumenlang.lumen.plugin.defaults.binder;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.annotations.Call;
import dev.lumenlang.lumen.api.annotations.Registration;
import dev.lumenlang.lumen.api.binder.ScriptAnnotationBinder;
import dev.lumenlang.lumen.plugin.annotations.LumenEvent;
import dev.lumenlang.lumen.plugin.events.EventSlots;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

/**
 * Binder that scans for {@link LumenEvent} annotations and registers
 * the annotated methods as Bukkit event listeners.
 */
@Registration(order = -100)
@SuppressWarnings("unused")
public final class EventAnnotationBinder implements ScriptAnnotationBinder {

    @Call
    public void register(@NotNull LumenAPI api) {
        api.binders().register(this);
    }

    @Override
    public void bind(@NotNull Object instance, @NotNull Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            LumenEvent evt = method.getAnnotation(LumenEvent.class);
            if (evt == null) continue;
            try {
                MethodHandle mh = MethodHandles.lookup().findVirtual(
                        clazz, method.getName(),
                        MethodType.methodType(void.class, evt.value()));
                MethodHandle adapted = mh.asType(
                        MethodType.methodType(void.class, Object.class, Event.class));
                EventSlots.bind(evt.value().asSubclass(Event.class), instance, adapted, EventPriority.valueOf(evt.priority()));
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    @Override
    public void unbind(@NotNull Object instance) {
        EventSlots.clearAll(instance);
    }
}
