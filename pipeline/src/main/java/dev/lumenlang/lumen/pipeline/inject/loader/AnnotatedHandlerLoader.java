package dev.lumenlang.lumen.pipeline.inject.loader;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.handler.ConditionHandler;
import dev.lumenlang.lumen.api.handler.ExpressionHandler;
import dev.lumenlang.lumen.api.handler.StatementHandler;
import dev.lumenlang.lumen.api.inject.index.IndexedHandler;
import dev.lumenlang.lumen.api.inject.index.IndexedMeta;
import dev.lumenlang.lumen.api.inject.index.IndexedParam;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.pattern.builder.ConditionBuilder;
import dev.lumenlang.lumen.api.pattern.builder.ExpressionBuilder;
import dev.lumenlang.lumen.api.pattern.builder.StatementBuilder;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.pipeline.inject.sidecar.SidecarConditionHandler;
import dev.lumenlang.lumen.pipeline.inject.sidecar.SidecarExpressionHandler;
import dev.lumenlang.lumen.pipeline.inject.sidecar.SidecarStatementHandler;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads {@code META-INF/lumen/handlers.json} from the given class loader and
 * registers each handler entry through the supplied {@link LumenAPI}. Each
 * registration produces a {@link SidecarStatementHandler}, {@link SidecarExpressionHandler},
 * or {@link SidecarConditionHandler} that emits the preserved source body
 * verbatim at codegen time.
 */
public final class AnnotatedHandlerLoader {

    private AnnotatedHandlerLoader() {
    }

    public static void load(@NotNull LumenAPI api, @NotNull ClassLoader loader, @NotNull String owner) {
        List<IndexedHandler> entries;
        try {
            entries = IndexLoader.load(loader);
        } catch (IOException e) {
            LumenLogger.warning("Failed to read META-INF/lumen/handlers.json for '" + owner + "': " + e.getMessage());
            return;
        }
        if (entries.isEmpty()) return;

        try {
            SidecarReader.load(loader);
        } catch (IOException e) {
            LumenLogger.warning("Failed to read META-INF/lumen/sources.gson.gz for '" + owner + "': " + e.getMessage());
        }

        int registered = 0;
        for (IndexedHandler entry : entries) {
            try {
                registerOne(api, owner, entry);
                registered++;
            } catch (Throwable t) {
                LumenLogger.warning("Failed to register annotation handler '" + entry.method() + "' from '" + owner + "': " + t.getMessage());
            }
        }
        if (registered > 0) {
            LumenLogger.info("Loaded " + registered + " annotation-form handler(s) from '" + owner + "'");
        }
    }

    private static void registerOne(@NotNull LumenAPI api, @NotNull String owner, @NotNull IndexedHandler entry) {
        switch (entry.kind()) {
            case "Statement" -> api.patterns().statement(b -> applyStatement(b, owner, entry));
            case "Expression" -> api.patterns().expression(b -> applyExpression(b, owner, entry));
            case "Condition" -> api.patterns().condition(b -> applyCondition(b, owner, entry));
            default -> throw new IllegalStateException("Unknown handler kind: " + entry.kind());
        }
    }

    private static void applyStatement(@NotNull StatementBuilder b, @NotNull String owner, @NotNull IndexedHandler entry) {
        applyCommon(b, owner, entry);
        StatementHandler handler = new SidecarStatementHandler(entry.owner(), entry.method(), entry.descriptor(), placeholderNames(entry));
        b.handler(handler);
    }

    private static void applyExpression(@NotNull ExpressionBuilder b, @NotNull String owner, @NotNull IndexedHandler entry) {
        applyCommon(b, owner, entry);
        ExpressionHandler handler = new SidecarExpressionHandler(entry.owner(), entry.method(), entry.descriptor(), placeholderNames(entry), expressionReturnType(entry));
        b.handler(handler);
    }

    private static void applyCondition(@NotNull ConditionBuilder b, @NotNull String owner, @NotNull IndexedHandler entry) {
        applyCommon(b, owner, entry);
        ConditionHandler handler = new SidecarConditionHandler(entry.owner(), entry.method(), entry.descriptor(), placeholderNames(entry));
        b.handler(handler);
    }

    private static <B> void applyCommon(@NotNull Object builder, @NotNull String owner, @NotNull IndexedHandler entry) {
        if (builder instanceof StatementBuilder s) {
            s.by(owner);
            for (String pattern : entry.patterns()) s.pattern(pattern);
            applyMetaToStatement(s, entry.meta());
        } else if (builder instanceof ExpressionBuilder e) {
            e.by(owner);
            for (String pattern : entry.patterns()) e.pattern(pattern);
            applyMetaToExpression(e, entry.meta());
        } else if (builder instanceof ConditionBuilder c) {
            c.by(owner);
            for (String pattern : entry.patterns()) c.pattern(pattern);
            applyMetaToCondition(c, entry.meta());
        }
    }

    private static void applyMetaToStatement(@NotNull StatementBuilder b, @NotNull IndexedMeta meta) {
        if (meta.description() != null) b.description(meta.description());
        for (String ex : meta.examples()) b.example(ex);
        if (meta.since() != null) b.since(meta.since());
        if (meta.category() != null) b.category(Categories.createOrGet(meta.category()));
        if (meta.deprecated()) b.deprecated(true);
    }

    private static void applyMetaToExpression(@NotNull ExpressionBuilder b, @NotNull IndexedMeta meta) {
        if (meta.description() != null) b.description(meta.description());
        for (String ex : meta.examples()) b.example(ex);
        if (meta.since() != null) b.since(meta.since());
        if (meta.category() != null) b.category(Categories.createOrGet(meta.category()));
        if (meta.deprecated()) b.deprecated(true);
    }

    private static void applyMetaToCondition(@NotNull ConditionBuilder b, @NotNull IndexedMeta meta) {
        if (meta.description() != null) b.description(meta.description());
        for (String ex : meta.examples()) b.example(ex);
        if (meta.since() != null) b.since(meta.since());
        if (meta.category() != null) b.category(Categories.createOrGet(meta.category()));
        if (meta.deprecated()) b.deprecated(true);
    }

    private static @NotNull List<String> placeholderNames(@NotNull IndexedHandler entry) {
        List<String> names = new ArrayList<>(entry.params().size());
        for (IndexedParam p : entry.params()) names.add(p.name());
        return List.copyOf(names);
    }

    /**
     * Resolves the Lumen return type for an expression handler from the
     * descriptor's return slot. Throws when the descriptor's Java type is not
     * in the registry; the handler's return type must be one Lumen knows
     * about.
     */
    private static @NotNull LumenType expressionReturnType(@NotNull IndexedHandler entry) {
        String desc = entry.descriptor();
        int close = desc.indexOf(')');
        String returnDesc = close < 0 ? "" : desc.substring(close + 1);
        String javaType = descriptorToJavaType(returnDesc);
        LumenType resolved = LumenType.fromJavaType(javaType);
        if (resolved == null) {
            throw new IllegalStateException("Expression handler " + entry.owner() + "#" + entry.method() + " returns '" + javaType + "' which is not a registered Lumen type");
        }
        return resolved;
    }

    private static @NotNull String descriptorToJavaType(@NotNull String descriptor) {
        return switch (descriptor) {
            case "I" -> "int";
            case "J" -> "long";
            case "D" -> "double";
            case "F" -> "float";
            case "Z" -> "boolean";
            case "B" -> "byte";
            case "S" -> "short";
            case "C" -> "char";
            case "V" -> "void";
            default -> {
                if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
                    yield descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
                }
                yield "java.lang.Object";
            }
        };
    }
}
