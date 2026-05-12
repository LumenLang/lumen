package dev.lumenlang.lumen.pipeline.inject.sidecar;

import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.inject.index.SidecarEntry;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds the placeholder-to-Java-expression map every sidecar handler feeds
 * into {@link BindingReplacer}, plus shared imports wiring.
 */
public final class SidecarBindings {

    private SidecarBindings() {
    }

    public static @NotNull Map<String, String> resolve(@NotNull HandlerContext ctx, @NotNull Iterable<String> placeholderNames) {
        Map<String, String> bindings = new LinkedHashMap<>();
        for (String name : placeholderNames) bindings.put(name, ctx.java(name));
        return bindings;
    }

    public static void addImports(@NotNull HandlerContext ctx, @NotNull SidecarEntry entry) {
        for (String imp : entry.imports()) {
            if (!imp.startsWith("java.lang.")) ctx.codegen().addImport(imp);
        }
    }
}
