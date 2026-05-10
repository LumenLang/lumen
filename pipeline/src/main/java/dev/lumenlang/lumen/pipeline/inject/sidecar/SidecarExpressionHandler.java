package dev.lumenlang.lumen.pipeline.inject.sidecar;

import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.handler.ExpressionHandler;
import dev.lumenlang.lumen.api.inject.index.IndexedParam;
import dev.lumenlang.lumen.api.inject.index.SidecarEntry;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.pipeline.inject.loader.SidecarReader;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Expression handler backed by a sidecar source. Inlines a single
 * {@code return X;} body as the expression. Multi-statement bodies fall
 * back to a generated static method on the script class.
 */
public final class SidecarExpressionHandler implements ExpressionHandler {

    private final @NotNull String owner;
    private final @NotNull String method;
    private final @NotNull String descriptor;
    private final @NotNull List<IndexedParam> params;
    private final @NotNull LumenType returnType;
    private final @NotNull String javaReturnType;

    public SidecarExpressionHandler(@NotNull String owner, @NotNull String method, @NotNull String descriptor, @NotNull List<IndexedParam> params, @NotNull LumenType returnType, @NotNull String javaReturnType) {
        this.owner = owner;
        this.method = method;
        this.descriptor = descriptor;
        this.params = params;
        this.returnType = returnType;
        this.javaReturnType = javaReturnType;
    }

    @Override
    public @NotNull ExpressionResult handle(@NotNull HandlerContext ctx) {
        SidecarEntry entry = SidecarReader.find(owner, method, descriptor);
        if (entry == null) {
            throw new IllegalStateException("No sidecar entry for expression handler " + owner + "#" + method + descriptor);
        }
        if (entry.returnExpression() != null) {
            SidecarBindings.addImports(ctx, entry);
            Map<String, String> bindings = SidecarBindings.resolve(ctx, placeholderNames());
            return new ExpressionResult(BindingReplacer.replace(entry.returnExpression(), bindings), returnType);
        }
        return new ExpressionResult(MethodEmitter.emit(ctx, entry, params, javaReturnType), returnType);
    }

    private @NotNull List<String> placeholderNames() {
        List<String> names = new ArrayList<>(params.size());
        for (IndexedParam p : params) names.add(p.name());
        return names;
    }
}
