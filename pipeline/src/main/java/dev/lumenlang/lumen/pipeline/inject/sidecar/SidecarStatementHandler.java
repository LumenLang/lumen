package dev.lumenlang.lumen.pipeline.inject.sidecar;

import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.handler.StatementHandler;
import dev.lumenlang.lumen.api.inject.index.SidecarEntry;
import dev.lumenlang.lumen.pipeline.inject.loader.SidecarReader;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Statement handler that emits a method body preserved by the build plugin
 * in {@code META-INF/lumen/sources.gson.gz}. The handler binding name list
 * comes from the handler entry index; each placeholder is replaced with
 * {@code ctx.java(name)} when emitting.
 */
public final class SidecarStatementHandler implements StatementHandler {

    private final @NotNull String owner;
    private final @NotNull String method;
    private final @NotNull String descriptor;
    private final @NotNull List<String> placeholderNames;

    public SidecarStatementHandler(@NotNull String owner, @NotNull String method, @NotNull String descriptor, @NotNull List<String> placeholderNames) {
        this.owner = owner;
        this.method = method;
        this.descriptor = descriptor;
        this.placeholderNames = placeholderNames;
    }

    @Override
    public void handle(@NotNull HandlerContext ctx) {
        SidecarEntry entry = SidecarReader.find(owner, method, descriptor);
        if (entry == null) {
            throw new IllegalStateException("No sidecar entry for handler " + owner + "#" + method + descriptor);
        }
        SidecarBindings.addImports(ctx, entry);
        Map<String, String> bindings = SidecarBindings.resolve(ctx, placeholderNames);
        for (String line : entry.runtimeBodyLines()) {
            ctx.out().line(BindingReplacer.replace(line, bindings));
        }
    }
}
