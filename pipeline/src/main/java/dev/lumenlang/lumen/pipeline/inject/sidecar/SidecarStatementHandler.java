package dev.lumenlang.lumen.pipeline.inject.sidecar;

import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.handler.StatementHandler;
import dev.lumenlang.lumen.api.inject.index.IndexedParam;
import dev.lumenlang.lumen.api.inject.index.SidecarEntry;
import dev.lumenlang.lumen.pipeline.inject.loader.SidecarReader;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Statement handler that emits a body preserved by the build plugin in
 * {@code META-INF/lumen/sources.gson.gz}. Inlines the body at the call site
 * by default; switches to a generated static method on the script class when
 * the handler carries {@code @MethodBased}.
 */
public final class SidecarStatementHandler implements StatementHandler {

    private final @NotNull String owner;
    private final @NotNull String method;
    private final @NotNull String descriptor;
    private final @NotNull List<IndexedParam> params;
    private final boolean methodBased;

    public SidecarStatementHandler(@NotNull String owner, @NotNull String method, @NotNull String descriptor, @NotNull List<IndexedParam> params, boolean methodBased) {
        this.owner = owner;
        this.method = method;
        this.descriptor = descriptor;
        this.params = params;
        this.methodBased = methodBased;
    }

    @Override
    public void handle(@NotNull HandlerContext ctx) {
        SidecarEntry entry = SidecarReader.find(owner, method, descriptor);
        if (entry == null) {
            throw new IllegalStateException("No sidecar entry for handler " + owner + "#" + method + descriptor);
        }
        if (methodBased) {
            ctx.out().line(MethodEmitter.emit(ctx, entry, params, "void") + ";");
            return;
        }
        SidecarBindings.addImports(ctx, entry);
        Map<String, String> bindings = SidecarBindings.resolve(ctx, placeholderNames());
        for (String line : entry.runtimeBodyLines()) {
            ctx.out().line(BindingReplacer.replace(line, bindings));
        }
    }

    private @NotNull List<String> placeholderNames() {
        List<String> names = new ArrayList<>(params.size());
        for (IndexedParam p : params) names.add(p.name());
        return names;
    }
}
