package dev.lumenlang.lumen.pipeline.inject.bytecode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global registry of injectable methods that need bytecode injection after compilation.
 *
 * <p>Each script class can have multiple injectable methods registered. After the Java
 * compiler produces bytecode, the {@link BytecodeInjector} uses this registry to find
 * which methods need their bodies replaced.
 */
public final class InjectableRegistry {

    private static final Map<String, List<InjectableMethod>> METHODS = new ConcurrentHashMap<>();

    /**
     * Registers an injectable method for the given script class.
     *
     * @param scriptClassName the fully qualified name of the script class
     * @param method the injectable method descriptor
     */
    public static void register(@NotNull String scriptClassName, @NotNull InjectableMethod method) {
        METHODS.computeIfAbsent(scriptClassName, k -> Collections.synchronizedList(new ArrayList<>())).add(method);
    }

    /**
     * Returns all injectable methods registered for the given script class, or null if none.
     *
     * @param scriptClassName the fully qualified name of the script class
     * @return the list of injectable methods, or null
     */
    public static @Nullable List<InjectableMethod> methods(@NotNull String scriptClassName) {
        return METHODS.get(scriptClassName);
    }

    /**
     * Removes all injectable methods for the given script class.
     *
     * @param scriptClassName the fully qualified name of the script class
     */
    public static void clear(@NotNull String scriptClassName) {
        METHODS.remove(scriptClassName);
    }
}
