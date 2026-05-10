package dev.lumenlang.build.emit.map;

import dev.lumenlang.build.scan.handler.HandlerKind;
import dev.lumenlang.build.scan.handler.HandlerMeta;
import dev.lumenlang.build.scan.handler.ScannedHandler;
import dev.lumenlang.build.scan.param.InjectParam;
import dev.lumenlang.build.validate.inject.Placeholders;
import dev.lumenlang.lumen.api.inject.index.IndexedHandler;
import dev.lumenlang.lumen.api.inject.index.IndexedMeta;
import dev.lumenlang.lumen.api.inject.index.IndexedParam;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds an {@link IndexedHandler} entry from a scanner result. Resolves
 * each parameter's binding id by consulting the merged placeholder map of
 * the handler's pattern(s).
 */
public final class IndexMapper {

    private IndexMapper() {
    }

    public static @NotNull IndexedHandler map(@NotNull ScannedHandler scanned) {
        Map<String, String> placeholders = mergedPlaceholders(scanned.patterns());
        List<IndexedParam> params = new ArrayList<>(scanned.injectParams().size());
        for (InjectParam p : scanned.injectParams()) {
            String name = p.placeholderName();
            String binding = placeholders.getOrDefault(name, "");
            params.add(new IndexedParam(name, binding, p.javaType()));
        }
        //return new IndexedHandler(kindLabel(scanned.kind()), scanned.ownerInternalName(), scanned.methodName(), scanned.methodDescriptor(), List.copyOf(scanned.patterns()), List.copyOf(params), mapMeta(scanned.meta()), scanned.methodBased());
        return new IndexedHandler(kindLabel(scanned.kind()), scanned.ownerInternalName(), scanned.methodName(), scanned.methodDescriptor(), List.copyOf(scanned.patterns()), List.copyOf(params), mapMeta(scanned.meta()));
    }

    private static @NotNull String kindLabel(@NotNull HandlerKind kind) {
        return switch (kind) {
            case STATEMENT -> "Statement";
            case EXPRESSION -> "Expression";
            case CONDITION -> "Condition";
        };
    }

    private static @NotNull IndexedMeta mapMeta(@NotNull HandlerMeta meta) {
        return new IndexedMeta(meta.description(), meta.examples(), meta.since(), meta.category(), meta.deprecated());
    }

    private static @NotNull Map<String, String> mergedPlaceholders(@NotNull List<String> patterns) {
        Map<String, String> merged = new HashMap<>();
        for (String pattern : patterns) merged.putAll(Placeholders.bindings(pattern));
        return merged;
    }
}
