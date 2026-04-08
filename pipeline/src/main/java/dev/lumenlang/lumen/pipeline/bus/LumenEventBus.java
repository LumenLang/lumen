package dev.lumenlang.lumen.pipeline.bus;

import dev.lumenlang.lumen.api.bus.Async;
import dev.lumenlang.lumen.api.bus.EventBus;
import dev.lumenlang.lumen.api.bus.LumenEvent;
import dev.lumenlang.lumen.api.bus.Priority;
import dev.lumenlang.lumen.api.bus.Subscribe;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Default {@link EventBus} implementation.
 */
public final class LumenEventBus implements EventBus {

    private static final ExecutorService ASYNC_POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "Lumen-EventBus-Async");
        t.setDaemon(true);
        return t;
    });

    private final Map<Class<?>, List<Subscriber>> subscribersByType = new ConcurrentHashMap<>();

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
            boolean async = method.isAnnotationPresent(Async.class);
            Subscriber sub = new Subscriber(listener, method, annotation.priority(), async, params[0]);
            subscribersByType.computeIfAbsent(params[0], k -> new CopyOnWriteArrayList<>()).add(sub);
            sortSubscribers(params[0]);
        }
    }

    @Override
    public void unregister(@NotNull Object listener) {
        for (List<Subscriber> subs : subscribersByType.values()) {
            subs.removeIf(s -> s.instance == listener);
        }
    }

    @Override
    public <T extends LumenEvent> @NotNull T post(@NotNull T event) {
        dispatch(event, false);
        return event;
    }

    @Override
    public <T extends LumenEvent> void postAsync(@NotNull T event) {
        ASYNC_POOL.execute(() -> dispatch(event, true));
    }

    /**
     * Shuts down the async thread pool. Called on plugin disable.
     */
    public void shutdown() {
        ASYNC_POOL.shutdownNow();
        subscribersByType.clear();
    }

    private void dispatch(@NotNull LumenEvent event, boolean forceAsync) {
        List<Subscriber> subs = subscribersByType.get(event.getClass());
        if (subs == null || subs.isEmpty()) return;
        for (Subscriber sub : subs) {
            if (forceAsync || sub.async) {
                if (forceAsync) {
                    invoke(sub, event);
                } else {
                    ASYNC_POOL.execute(() -> invoke(sub, event));
                }
            } else {
                invoke(sub, event);
            }
        }
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

    private record Subscriber(@NotNull Object instance, @NotNull Method method, @NotNull Priority priority, boolean async, @NotNull Class<?> eventType) {
    }
}
