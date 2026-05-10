package dev.lumenlang.lumen.pipeline.inject.sidecar;

import dev.lumenlang.lumen.api.codegen.CodegenContext;
import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.inject.index.IndexedParam;
import dev.lumenlang.lumen.api.inject.index.SidecarEntry;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Emits a static method onto the script class and returns a call expression
 * to it. Used when a handler body has more than a single statement so the
 * runtime body cannot be inlined as a value.
 *
 * <p>Each handler emits at most one method per script: subsequent calls in
 * the same script reuse the previously chosen name. The method name is the
 * handler's own method name with each {@code @Inject} parameter's simple
 * class name appended (first letter uppercased). On collision with a name
 * already used in the script class, a numeric suffix ({@code 2}, {@code 3},
 * ...) is appended.
 */
public final class MethodEmitter {

    private static final Map<CodegenContext, Map<String, String>> CACHED_NAMES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<CodegenContext, Set<String>> USED_NAMES = Collections.synchronizedMap(new WeakHashMap<>());

    private MethodEmitter() {
    }

    public static @NotNull String emit(@NotNull HandlerContext ctx, @NotNull SidecarEntry entry, @NotNull List<IndexedParam> params, @NotNull String returnType) {
        SidecarBindings.addImports(ctx, entry);
        addParamImports(ctx, params);
        addImportFor(ctx, returnType);

        String key = entry.owner() + "#" + entry.method() + entry.descriptor();
        Map<String, String> namesForCtx = CACHED_NAMES.computeIfAbsent(ctx.codegen(), k -> new HashMap<>());
        String methodName = namesForCtx.get(key);
        if (methodName == null) {
            methodName = pickName(ctx.codegen(), entry, params);
            namesForCtx.put(key, methodName);
            ctx.codegen().addMethod(buildMethodSource(methodName, entry, params, simpleNameOf(returnType)));
        }

        StringBuilder call = new StringBuilder();
        call.append(methodName).append('(');
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) call.append(", ");
            call.append(ctx.java(params.get(i).name()));
        }
        call.append(')');
        return call.toString();
    }

    private static @NotNull String pickName(@NotNull CodegenContext codegen, @NotNull SidecarEntry entry, @NotNull List<IndexedParam> params) {
        String base = baseName(entry.method(), params);
        Set<String> used = USED_NAMES.computeIfAbsent(codegen, k -> new HashSet<>());
        if (used.add(base)) return base;
        int suffix = 2;
        while (!used.add(base + suffix)) suffix++;
        return base + suffix;
    }

    private static @NotNull String baseName(@NotNull String methodName, @NotNull List<IndexedParam> params) {
        StringBuilder sb = new StringBuilder(methodName);
        for (IndexedParam p : params) sb.append(capitalize(Descriptors.simpleNameOf(p.descriptor())));
        return sb.toString();
    }

    private static @NotNull String capitalize(@NotNull String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static @NotNull String buildMethodSource(@NotNull String name, @NotNull SidecarEntry entry, @NotNull List<IndexedParam> params, @NotNull String returnTypeSimple) {
        StringBuilder sb = new StringBuilder();
        sb.append("private static ").append(returnTypeSimple).append(' ').append(name).append('(');
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(Descriptors.simpleNameOf(params.get(i).descriptor())).append(' ').append(params.get(i).name());
        }
        sb.append(") {\n");
        for (String line : entry.runtimeBodyLines()) sb.append("    ").append(line).append('\n');
        sb.append('}');
        return sb.toString();
    }

    private static void addParamImports(@NotNull HandlerContext ctx, @NotNull List<IndexedParam> params) {
        for (IndexedParam p : params) {
            String javaType = Descriptors.javaTypeOf(p.descriptor());
            if (javaType.contains(".") && !javaType.startsWith("java.lang.")) {
                ctx.codegen().addImport(javaType);
            }
        }
    }

    private static void addImportFor(@NotNull HandlerContext ctx, @NotNull String javaType) {
        if (javaType.contains(".") && !javaType.startsWith("java.lang.")) {
            ctx.codegen().addImport(javaType);
        }
    }

    private static @NotNull String simpleNameOf(@NotNull String javaType) {
        int dot = javaType.lastIndexOf('.');
        return dot < 0 ? javaType : javaType.substring(dot + 1);
    }
}
