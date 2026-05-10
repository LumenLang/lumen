package dev.lumenlang.lumen.pipeline.inject.sidecar;

import dev.lumenlang.lumen.api.codegen.HandlerContext;
import dev.lumenlang.lumen.api.handler.ConditionHandler;
import dev.lumenlang.lumen.api.inject.index.SidecarEntry;
import dev.lumenlang.lumen.pipeline.inject.loader.SidecarReader;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Condition handler backed by a sidecar source. Requires the runtime body
 * to be a single {@code return X;} so the boolean expression can be lifted
 * verbatim into the script's condition slot.
 */
public final class SidecarConditionHandler implements ConditionHandler {

    private final @NotNull String owner;
    private final @NotNull String method;
    private final @NotNull String descriptor;
    private final @NotNull List<String> placeholderNames;

    public SidecarConditionHandler(@NotNull String owner, @NotNull String method, @NotNull String descriptor, @NotNull List<String> placeholderNames) {
        this.owner = owner;
        this.method = method;
        this.descriptor = descriptor;
        this.placeholderNames = placeholderNames;
    }

    @Override
    public @NotNull String handle(@NotNull HandlerContext ctx) {
        SidecarEntry entry = SidecarReader.find(owner, method, descriptor);
        if (entry == null) {
            throw new IllegalStateException("No sidecar entry for condition handler " + owner + "#" + method + descriptor);
        }
        if (entry.returnExpression() == null) {
            throw new IllegalStateException("Condition handler " + owner + "#" + method + " must be a single return statement");
        }
        SidecarBindings.addImports(ctx, entry);
        Map<String, String> bindings = SidecarBindings.resolve(ctx, placeholderNames);
        return BindingReplacer.replace(entry.returnExpression(), bindings);
    }
}
