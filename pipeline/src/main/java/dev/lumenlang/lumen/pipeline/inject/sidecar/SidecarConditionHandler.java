package dev.lumenlang.lumen.pipeline.inject.sidecar;

import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.handler.ConditionHandler;
import dev.lumenlang.lumen.api.inject.index.IndexedParam;
import dev.lumenlang.lumen.api.inject.index.SidecarEntry;
import dev.lumenlang.lumen.pipeline.inject.loader.SidecarReader;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Condition handler backed by a sidecar source. Inlines a single
 * {@code return X;} body as the boolean expression. Multi-statement bodies
 * fall back to a generated static method on the script class.
 */
public final class SidecarConditionHandler implements ConditionHandler {

    private final @NotNull String owner;
    private final @NotNull String method;
    private final @NotNull String descriptor;
    private final @NotNull List<IndexedParam> params;

    public SidecarConditionHandler(@NotNull String owner, @NotNull String method, @NotNull String descriptor, @NotNull List<IndexedParam> params) {
        this.owner = owner;
        this.method = method;
        this.descriptor = descriptor;
        this.params = params;
    }

    @Override
    public @NotNull String handle(@NotNull HandlerContext ctx) {
        SidecarEntry entry = SidecarReader.find(owner, method, descriptor);
        if (entry == null) {
            throw new IllegalStateException("No sidecar entry for condition handler " + owner + "#" + method + descriptor);
        }
        if (entry.returnExpression() != null) {
            SidecarBindings.addImports(ctx, entry);
            Map<String, String> bindings = SidecarBindings.resolve(ctx, placeholderNames());
            return BindingReplacer.replace(entry.returnExpression(), bindings);
        }
        return MethodEmitter.emit(ctx, entry, params, "boolean");
    }

    private @NotNull List<String> placeholderNames() {
        List<String> names = new ArrayList<>(params.size());
        for (IndexedParam p : params) names.add(p.name());
        return names;
    }
}
