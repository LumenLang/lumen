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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Default {@link EventBus} implementation.
 *
 * <p>Subscribers registered for a supertype or interface of an event will
 * receive that event. A dispatch cache avoids repeated hierarchy traversal.
 */
public final class LumenEventBus implements EventBus {

    private final ExecutorService asyncPool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "Lumen-EventBus-Async");
        t.setDaemon(true);
        return t;
    });

    private final Map<Class<?>, List<Subscriber>> subscribersByType = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Subscriber>> dispatchCache = new ConcurrentHashMap<>();

    @Override
    public void register(@NotNull Object listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            Subscribe annotation = method.getAnnotation(Subscribe.class);
            if (annotation == null) continue;
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 1 || !LumenEvent.class.isAssignableFrom(params[0])) {
                LumenLogger.warning("@Subscribe method " + listener.getClass().getName() + "#" + method.getName() + " must accept exactly one LumenEvent parameter, skipping");
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

    @Override
    public <T extends LumenEvent> void postAsync(@NotNull T event) {
        asyncPool.execute(() -> {
            for (Subscriber sub : resolveSubscribers(event.getClass())) {
                invoke(sub, event);
            }
        });
    }

    /**
     * Clears all subscribers and shuts down the async pool. Called on plugin disable.
     */
    public void shutdown() {
        subscribersByType.clear();
        dispatchCache.clear();
        asyncPool.shutdownNow();
    }

    private @NotNull List<Subscriber> resolveSubscribers(@NotNull Class<?> eventType) {
        return dispatchCache.computeIfAbsent(eventType, this::buildSubscriberList);
    }

    private @NotNull List<Subscriber> buildSubscriberList(@NotNull Class<?> eventType) {
        List<Subscriber> all = new ArrayList<>();
        Class<?> cls = eventType;
        while (cls != null && cls != Object.class) {
            List<Subscriber> direct = subscribersByType.get(cls);
            if (direct != null) all.addAll(direct);
            for (Class<?> iface : cls.getInterfaces()) collectFromInterface(iface, all);
            cls = cls.getSuperclass();
        }
        all.sort(Comparator.comparingInt(s -> s.priority.ordinal()));
        return List.copyOf(all);
    }

    private void collectFromInterface(@NotNull Class<?> iface, @NotNull List<Subscriber> out) {
        List<Subscriber> subs = subscribersByType.get(iface);
        if (subs != null) out.addAll(subs);
        for (Class<?> parent : iface.getInterfaces()) collectFromInterface(parent, out);
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

