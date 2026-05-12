package dev.lumenlang.lumen.pipeline.inject.sidecar;

import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.handler.ConditionHandler;
import dev.lumenlang.lumen.api.inject.index.IndexedHandler;
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

    private final @NotNull IndexedHandler indexed;
    private final @NotNull ClassLoader addonLoader;
    private final @NotNull List<IndexedParam> params;

    public SidecarConditionHandler(@NotNull IndexedHandler indexed, @NotNull ClassLoader addonLoader) {
        this.indexed = indexed;
        this.addonLoader = addonLoader;
        this.params = indexed.params();
    }

    @Override
    public @NotNull String handle(@NotNull HandlerContext ctx) {
        SidecarEntry entry = SidecarReader.find(indexed.owner(), indexed.method(), indexed.descriptor());
        if (entry == null) {
            throw new IllegalStateException("No sidecar entry for condition handler " + indexed.owner() + "#" + indexed.method() + indexed.descriptor());
        }
        if (entry.wantsContext()) CompileSectionInvoker.invoke(addonLoader, indexed, ctx);
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
