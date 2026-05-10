package dev.lumenlang.lumen.pipeline.inject.sidecar;

import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.handler.ExpressionHandler;
import dev.lumenlang.lumen.api.inject.index.SidecarEntry;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.pipeline.inject.loader.SidecarReader;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Expression handler backed by a sidecar source. Requires the runtime body
 * to be a single {@code return X;}; the expression and Lumen return type are
 * lifted into the script's expression slot.
 */
public final class SidecarExpressionHandler implements ExpressionHandler {

    private final @NotNull String owner;
    private final @NotNull String method;
    private final @NotNull String descriptor;
    private final @NotNull List<String> placeholderNames;
    private final @NotNull LumenType returnType;

    public SidecarExpressionHandler(@NotNull String owner, @NotNull String method, @NotNull String descriptor, @NotNull List<String> placeholderNames, @NotNull LumenType returnType) {
        this.owner = owner;
        this.method = method;
        this.descriptor = descriptor;
        this.placeholderNames = placeholderNames;
        this.returnType = returnType;
    }

    @Override
    public @NotNull ExpressionResult handle(@NotNull HandlerContext ctx) {
        SidecarEntry entry = SidecarReader.find(owner, method, descriptor);
        if (entry == null) {
            throw new IllegalStateException("No sidecar entry for expression handler " + owner + "#" + method + descriptor);
        }
        if (entry.returnExpression() == null) {
            throw new IllegalStateException("Expression handler " + owner + "#" + method + " must be a single return statement");
        }
        SidecarBindings.addImports(ctx, entry);
        Map<String, String> bindings = SidecarBindings.resolve(ctx, placeholderNames);
        return new ExpressionResult(BindingReplacer.replace(entry.returnExpression(), bindings), returnType);
    }
}
