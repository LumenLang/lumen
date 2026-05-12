package dev.lumenlang.lumen.pipeline.inject.sidecar;

import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.diagnostic.DiagnosticException;
import dev.lumenlang.lumen.api.diagnostic.LumenDiagnostic;
import dev.lumenlang.lumen.api.inject.index.IndexedHandler;
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
 * first parameter is a {@link HandlerContext}.
 */
public final class CompileSectionInvoker {

    private static final Map<String, Object> CACHE = new ConcurrentHashMap<>();

    private CompileSectionInvoker() {
    }

    public static void invoke(@NotNull ClassLoader addonLoader, @NotNull IndexedHandler entry, @NotNull HandlerContext ctx) {
        String key = entry.owner() + "#" + entry.method() + entry.descriptor();
        Object cached = CACHE.computeIfAbsent(key, k -> tryResolve(addonLoader, entry));
        if (cached instanceof FailedResolution failed) {
            throw new DiagnosticException(unresolvedDiagnostic(entry, ctx, failed.cause));
        }
        try {
            ((MethodHandle) cached).invokeWithArguments(arguments(entry, ctx));
        } catch (Throwable t) {
            throw new DiagnosticException(invokeFailedDiagnostic(entry, ctx, t));
        }
    }

    private static @NotNull Object tryResolve(@NotNull ClassLoader loader, @NotNull IndexedHandler entry) {
        String className = entry.owner().replace('/', '.');
        try {
            Class<?> owner = Class.forName(className, true, loader);
            List<String> argDescriptors = Descriptors.argumentDescriptors(entry.descriptor());
            Class<?>[] paramClasses = new Class<?>[argDescriptors.size()];
            for (int i = 0; i < argDescriptors.size(); i++)
                paramClasses[i] = Descriptors.classOf(argDescriptors.get(i), loader);
            Method method = owner.getDeclaredMethod(entry.method(), paramClasses);
            method.setAccessible(true);
            return MethodHandles.lookup().unreflect(method);
        } catch (ReflectiveOperationException e) {
            return new FailedResolution(e);
        }
    }

    private static @NotNull LumenDiagnostic unresolvedDiagnostic(@NotNull IndexedHandler entry, @NotNull HandlerContext ctx, @NotNull ReflectiveOperationException cause) {
        String simpleClass = simpleName(entry.owner());
        String causeText = cause.getClass().getSimpleName() + (cause.getMessage() != null ? ": " + cause.getMessage() : "");
        return LumenDiagnostic.error("This pattern points to a handler that no longer exists in the addon")
                .at(ctx.source().currentLine(), ctx.source().currentRaw())
                .label("handler missing at runtime")
                .note("the addon advertises a pattern backed by " + simpleClass + "#" + entry.method() + ", but that method cannot be loaded")
                .note("this is not a problem with your script; the addon shipped a stale handler index")
                .note("internal cause: " + causeText)
                .help("if you are the addon author: rebuild the addon from a clean state so the handler index matches the shipped classes")
                .help("if you are using this addon: report the issue to its author, or update to a newer version that fixes the index")
                .build();
    }

    private static @NotNull LumenDiagnostic invokeFailedDiagnostic(@NotNull IndexedHandler entry, @NotNull HandlerContext ctx, @NotNull Throwable cause) {
        String simpleClass = simpleName(entry.owner());
        String causeText = cause.getClass().getSimpleName() + (cause.getMessage() != null ? ": " + cause.getMessage() : "");
        return LumenDiagnostic.error("This pattern's handler crashed while compiling your script")
                .at(ctx.source().currentLine(), ctx.source().currentRaw())
                .label("addon handler threw during compilation")
                .note("the compile-time logic in " + simpleClass + "#" + entry.method() + " failed; this is a bug in the addon, not in your script")
                .note("thrown: " + causeText)
                .help("if you are the addon author: the compile section of this handler is throwing; fix it so it tolerates this input")
                .help("if you are using this addon: report this to its author with the script line above")
                .build();
    }

    private static @NotNull String simpleName(@NotNull String owner) {
        int slash = owner.lastIndexOf('/');
        return slash < 0 ? owner : owner.substring(slash + 1);
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

    private record FailedResolution(@NotNull ReflectiveOperationException cause) {
    }
}
