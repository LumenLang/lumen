package dev.lumenlang.lumen.debug.server.snippet;

import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.api.type.LumenTypeRegistry;
import dev.lumenlang.lumen.api.type.ObjectType;
import dev.lumenlang.lumen.api.type.PrimitiveType;
import dev.lumenlang.lumen.debug.hook.ScriptHooks;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Maps runtime values and {@link ScriptHooks.SnippetVarMeta} entries to
 * Lumen type metadata for the debug snippet pipeline.
 */
public final class SnippetTypeResolver {

    private static final Map<Class<?>, ScriptHooks.SnippetVarMeta> PRIMITIVE_META = Map.of(
            String.class, new ScriptHooks.SnippetVarMeta("String", null, "String", null),
            Integer.class, new ScriptHooks.SnippetVarMeta("Integer", null, "int", null),
            Double.class, new ScriptHooks.SnippetVarMeta("Double", null, "double", null),
            Long.class, new ScriptHooks.SnippetVarMeta("Long", null, "long", null),
            Float.class, new ScriptHooks.SnippetVarMeta("Float", null, "float", null),
            Boolean.class, new ScriptHooks.SnippetVarMeta("Boolean", null, "boolean", null)
    );

    private SnippetTypeResolver() {
    }

    public static @NotNull LumenType lumenTypeOf(@NotNull ScriptHooks.SnippetVarMeta meta) {
        if (meta.refTypeId() != null) {
            ObjectType ot = LumenTypeRegistry.byId(meta.refTypeId());
            if (ot != null) return ot;
        }
        if (meta.javaType() != null) {
            PrimitiveType p = PrimitiveType.fromJavaType(meta.javaType());
            if (p != null) return p;
        }
        return PrimitiveType.STRING;
    }

    /**
     * Walks the class hierarchy and interface tree of {@code value}, matching
     * each type against {@link LumenTypeRegistry}.
     */
    public static @NotNull ScriptHooks.SnippetVarMeta metaOf(@NotNull Object value) {
        ScriptHooks.SnippetVarMeta primitive = PRIMITIVE_META.get(value.getClass());
        if (primitive != null) return primitive;

        Deque<Class<?>> queue = new ArrayDeque<>();
        Set<Class<?>> visited = new HashSet<>();
        queue.add(value.getClass());
        while (!queue.isEmpty()) {
            Class<?> cls = queue.poll();
            if (cls == null || cls == Object.class || !visited.add(cls)) continue;
            ObjectType found = LumenTypeRegistry.fromJava(cls.getName());
            if (found != null)
                return new ScriptHooks.SnippetVarMeta(cls.getSimpleName(), found.id(), null, cls.getName());
            if (cls.getSuperclass() != null) queue.add(cls.getSuperclass());
            Collections.addAll(queue, cls.getInterfaces());
        }
        return new ScriptHooks.SnippetVarMeta("Object", null, null, null);
    }
}
