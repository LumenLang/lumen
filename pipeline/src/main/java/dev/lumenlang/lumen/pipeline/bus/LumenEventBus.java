package dev.lumenlang.lumen.pipeline.bus;

import dev.lumenlang.lumen.api.bus.EventBus;
import dev.lumenlang.lumen.api.bus.LumenEvent;
import dev.lumenlang.lumen.api.bus.Priority;
import dev.lumenlang.lumen.api.bus.Subscribe;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Default {@link EventBus} implementation.
 *
 * <p>Subscribers registered for a supertype or interface of an event will
 * receive that event. A dispatch cache avoids repeated hierarchy traversal.
 * Diamond-inheritance is handled by tracking visited types during cache build.
 */
public final class LumenEventBus implements EventBus {

    private final Map<Class<?>, List<Subscriber>> subscribersByType = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Subscriber>> dispatchCache = new ConcurrentHashMap<>();

    @Override
    public void register(@NotNull Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            Subscribe annotation = method.getAnnotation(Subscribe.class);
            if (annotation == null) continue;
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 1 || (!LumenEvent.class.isAssignableFrom(params[0]) && !params[0].isInterface())) {
                LumenLogger.warning("@Subscribe method " + listener.getClass().getName() + "#" + method.getName() + " must accept exactly one LumenEvent or interface parameter, skipping");
                continue;
            }
            method.setAccessible(true);
            Subscriber sub = new Subscriber(listener, method, annotation.priority(), params[0]);
            subscribersByType.computeIfAbsent(params[0], k -> new CopyOnWriteArrayList<>()).add(sub);
            sortSubscribers(params[0]);
        }
        dispatchCache.clear();
    }

    @Override
    public void unregister(@NotNull Object listener) {
        for (List<Subscriber> subs : subscribersByType.values()) {
            subs.removeIf(s -> s.instance == listener);
        }
        dispatchCache.clear();
    }

    @Override
    public <T extends LumenEvent> @NotNull T post(@NotNull T event) {
        for (Subscriber sub : resolveSubscribers(event.getClass())) {
            invoke(sub, event);
        }
        return event;
    }

    /**
     * Clears all subscribers. Called on plugin disable.
     */
    public void shutdown() {
        subscribersByType.clear();
        dispatchCache.clear();
    }

    private @NotNull List<Subscriber> resolveSubscribers(@NotNull Class<?> eventType) {
        return dispatchCache.computeIfAbsent(eventType, this::buildSubscriberList);
    }

    private @NotNull List<Subscriber> buildSubscriberList(@NotNull Class<?> eventType) {
        List<Subscriber> all = new ArrayList<>();
        Set<Class<?>> visited = new HashSet<>();
        Class<?> cls = eventType;
        while (cls != null && cls != Object.class) {
            if (visited.add(cls)) {
                List<Subscriber> direct = subscribersByType.get(cls);
                if (direct != null) all.addAll(direct);
            }
            for (Class<?> iface : cls.getInterfaces()) collectFromInterface(iface, all, visited);
            cls = cls.getSuperclass();
        }
        all.sort(Comparator.comparingInt(s -> s.priority.ordinal()));
        return List.copyOf(all);
    }

    private void collectFromInterface(@NotNull Class<?> iface, @NotNull List<Subscriber> out, @NotNull Set<Class<?>> visited) {
        if (!visited.add(iface)) return;
        List<Subscriber> subs = subscribersByType.get(iface);
        if (subs != null) out.addAll(subs);
        for (Class<?> parent : iface.getInterfaces()) collectFromInterface(parent, out, visited);
    }

    private void invoke(@NotNull Subscriber sub, @NotNull LumenEvent event) {
        try {
            sub.method.invoke(sub.instance, event);
        } catch (Exception e) {
            LumenLogger.severe("Exception in event subscriber " + sub.instance.getClass().getName() + "#" + sub.method.getName(), e);
        }
    }

    private void sortSubscribers(@NotNull Class<?> eventType) {
        List<Subscriber> subs = subscribersByType.get(eventType);
        if (subs == null) return;
        List<Subscriber> sorted = subs.stream().sorted(Comparator.comparingInt(s -> s.priority.ordinal())).toList();
        subs.clear();
        subs.addAll(sorted);
    }

    private record Subscriber(@NotNull Object instance, @NotNull Method method, @NotNull Priority priority, @NotNull Class<?> eventType) {
    }
}


