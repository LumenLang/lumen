package dev.lumenlang.lumen.pipeline.inject.sidecar;

import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.inject.index.IndexedHandler;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves and invokes the trimmed bytecode of an annotated handler whose
 * first parameter is a {@link HandlerContext}. The build plugin removed the
 * runtime section from the original method, so calling it here runs only the
 * compile section's side effects on {@code ctx}. Inject parameters are passed
 * as defaults; the validator already forbids the compile section from reading
 * them.
 */
public final class CompileSectionInvoker {

    private static final Map<String, MethodHandle> CACHE = new ConcurrentHashMap<>();

    private CompileSectionInvoker() {
    }

    public static void invoke(@NotNull ClassLoader addonLoader, @NotNull IndexedHandler entry, @NotNull HandlerContext ctx) {
        String key = entry.owner() + "#" + entry.method() + entry.descriptor();
        MethodHandle handle = CACHE.computeIfAbsent(key, k -> resolve(addonLoader, entry));
        Object[] args = arguments(entry, ctx);
        try {
            handle.invokeWithArguments(args);
        } catch (Throwable t) {
            LumenLogger.warning("Compile section of " + entry.owner() + "#" + entry.method() + " threw: " + t);
        }
    }

    private static @NotNull MethodHandle resolve(@NotNull ClassLoader loader, @NotNull IndexedHandler entry) {
        String className = entry.owner().replace('/', '.');
        try {
            Class<?> owner = Class.forName(className, true, loader);
            List<String> argDescriptors = Descriptors.argumentDescriptors(entry.descriptor());
            Class<?>[] paramClasses = new Class<?>[argDescriptors.size()];
            for (int i = 0; i < argDescriptors.size(); i++) paramClasses[i] = Descriptors.classOf(argDescriptors.get(i), loader);
            Method method = owner.getDeclaredMethod(entry.method(), paramClasses);
            method.setAccessible(true);
            return MethodHandles.lookup().unreflect(method);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot resolve compile section for " + entry.owner() + "#" + entry.method() + entry.descriptor(), e);
        }
    }

    private static @NotNull Object @NotNull [] arguments(@NotNull IndexedHandler entry, @NotNull HandlerContext ctx) {
        List<String> argDescriptors = Descriptors.argumentDescriptors(entry.descriptor());
        Object[] args = new Object[argDescriptors.size()];
        args[0] = ctx;
        for (int i = 1; i < argDescriptors.size(); i++) args[i] = defaultValue(argDescriptors.get(i));
        return args;
    }

    private static @Nullable Object defaultValue(@NotNull String descriptor) {
        return switch (descriptor) {
            case "B" -> (byte) 0;
            case "S" -> (short) 0;
            case "C" -> (char) 0;
            case "I" -> 0;
            case "Z" -> false;
            case "J" -> 0L;
            case "F" -> 0f;
            case "D" -> 0d;
            default -> null;
        };
    }
}
