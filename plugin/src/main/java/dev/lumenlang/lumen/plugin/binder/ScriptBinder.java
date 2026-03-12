package dev.lumenlang.lumen.plugin.binder;

import dev.lumenlang.lumen.pipeline.annotations.LumenCmd;
import dev.lumenlang.lumen.pipeline.annotations.LumenEvent;
import dev.lumenlang.lumen.pipeline.annotations.LumenInventory;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.plugin.commands.CommandRegistry;
import dev.lumenlang.lumen.plugin.events.EventSlots;
import dev.lumenlang.lumen.plugin.util.InventoryRegistry;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Handles runtime binding of compiled script instances to Bukkit event and
 * commands.
 *
 * <p>
 * After a script class is compiled and loaded, this class reflectively scans
 * for
 * {@link LumenEvent} and {@link LumenCmd} annotations and wires them up to the
 * Bukkit event system and command registry.
 */
public final class ScriptBinder {

    /**
     * Binds an event handler at runtime using MethodHandles for a direct call site.
     *
     * @param instance   the script instance to call the method on
     * @param eventClass the class of the event to listen for
     * @param methodName the name of the method to call when the event is fired
     */
    public static void bindEvent(
            @NotNull Object instance,
            @NotNull Class<?> eventClass,
            @NotNull String methodName) {
        try {
            MethodHandle mh = MethodHandles.lookup().findVirtual(
                    instance.getClass(), methodName,
                    MethodType.methodType(void.class, eventClass));
            MethodHandle adapted = mh.asType(
                    MethodType.methodType(void.class, Object.class, Event.class));
            // noinspection unchecked
            EventSlots.bind((Class<? extends Event>) eventClass, instance, adapted);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /**
     * Scans the given class for {@link LumenEvent} and {@link LumenCmd} annotations
     * and
     * binds each annotated method to its corresponding event or command.
     *
     * @param inst  the script instance
     * @param clazz the script class
     */
    public static void bindAll(@NotNull Object inst, @NotNull Class<?> clazz) {
        for (Method m : clazz.getDeclaredMethods()) {
            LumenEvent evt = m.getAnnotation(LumenEvent.class);
            if (evt != null) {
                bindEvent(inst, evt.value(), m.getName());
            }

            LumenCmd cmd = m.getAnnotation(LumenCmd.class);
            if (cmd != null) {
                CommandRegistry.register(
                        cmd.scriptName(),
                        cmd.name(),
                        cmd.description().isEmpty() ? null : cmd.description(),
                        cmd.aliases().length == 0 ? null : Arrays.asList(cmd.aliases()),
                        cmd.permission().isEmpty() ? null : cmd.permission(),
                        cmd.namespace().equals("lumen") ? null : cmd.namespace(),
                        inst,
                        m);
            }

            LumenInventory inv = m.getAnnotation(LumenInventory.class);
            if (inv != null) {
                try {
                    MethodHandle mh = MethodHandles.lookup().findVirtual(
                            clazz, m.getName(),
                            MethodType.methodType(void.class, Player.class));
                    InventoryRegistry.register(inv.value(), inst, mh);
                } catch (Throwable t) {
                    LumenLogger.severe("Failed to bind inventory builder: " + inv.value(), t);
                }
            }
        }
    }

    /**
     * Unbinds all event listeners registered by the given script instance.
     *
     * @param inst the script instance to unbind
     */
    public static void unbindAll(@NotNull Object inst) {
        EventSlots.clearAll(inst);
        InventoryRegistry.clearInstance(inst);
    }

    /**
     * Invokes all methods annotated with the specified annotations type on the
     * given class instance.
     *
     * @param instance       the script instance
     * @param clazz          the script class
     * @param annotationType the annotations type to look for
     */
    public static void invokeMethodWithAnnotation(@NotNull Object instance,
                                                  @NotNull Class<?> clazz,
                                                  @NotNull Class<? extends Annotation> annotationType) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.isAnnotationPresent(annotationType)) {
                try {
                    m.invoke(instance);
                } catch (Throwable t) {
                    LumenLogger.severe("Failed to invoke @" + annotationType.getSimpleName()
                            + " method " + m.getName() + " on " + clazz.getName(), t);
                }
            }
        }
    }
}
