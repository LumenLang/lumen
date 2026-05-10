package dev.lumenlang.lumen.pipeline.inject.loader;

import dev.lumenlang.lumen.api.LumenAPI;
import dev.lumenlang.lumen.api.inject.index.IndexedHandler;
import dev.lumenlang.lumen.api.inject.index.IndexedMeta;
import dev.lumenlang.lumen.api.pattern.Categories;
import dev.lumenlang.lumen.api.pattern.Category;
import dev.lumenlang.lumen.api.pattern.builder.ConditionBuilder;
import dev.lumenlang.lumen.api.pattern.builder.ExpressionBuilder;
import dev.lumenlang.lumen.api.pattern.builder.StatementBuilder;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Reads {@code META-INF/lumen/handlers.json} from the given class loader and
 * registers each handler entry through the supplied {@link LumenAPI}, using
 * the existing builder API plus the {@code injectableHandler(Class, String)}
 * static-method form.
 *
 * <p>The build plugin has already pre-faked each handler's bytecode: a
 * {@code @Inject} parameter load is now a {@code Fakes.fakeXxx("name")} call
 * that the runtime extractor consumes through its existing path.
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
                registerOne(api, loader, owner, entry);
                registered++;
            } catch (Throwable t) {
                LumenLogger.warning("Failed to register annotation handler '" + entry.method() + "' from '" + owner + "': " + t.getMessage());
            }
        }
        if (registered > 0) {
            LumenLogger.info("Loaded " + registered + " annotation-form handler(s) from '" + owner + "'");
        }
    }

    private static void registerOne(@NotNull LumenAPI api, @NotNull ClassLoader loader, @NotNull String owner, @NotNull IndexedHandler entry) throws ClassNotFoundException {
        Class<?> ownerClass = Class.forName(entry.owner().replace('/', '.'), false, loader);
        switch (entry.kind()) {
            case "Statement" -> api.patterns().statement(b -> applyStatement(b, owner, entry, ownerClass));
            case "Expression" -> api.patterns().expression(b -> applyExpression(b, owner, entry, ownerClass));
            case "Condition" -> api.patterns().condition(b -> applyCondition(b, owner, entry, ownerClass));
            default -> throw new IllegalStateException("Unknown handler kind: " + entry.kind());
        }
    }

    private static void applyStatement(@NotNull StatementBuilder b, @NotNull String owner, @NotNull IndexedHandler entry, @NotNull Class<?> ownerClass) {
        b.by(owner);
        for (String pattern : entry.patterns()) b.pattern(pattern);
        applyMeta(entry.meta(), b::description, b::example, b::since, b::category, b::deprecated);
        b.injectableHandler(ownerClass, entry.method());
    }

    private static void applyExpression(@NotNull ExpressionBuilder b, @NotNull String owner, @NotNull IndexedHandler entry, @NotNull Class<?> ownerClass) {
        b.by(owner);
        for (String pattern : entry.patterns()) b.pattern(pattern);
        applyMeta(entry.meta(), b::description, b::example, b::since, b::category, b::deprecated);
        b.injectableHandler(ownerClass, entry.method());
    }

    private static void applyCondition(@NotNull ConditionBuilder b, @NotNull String owner, @NotNull IndexedHandler entry, @NotNull Class<?> ownerClass) {
        b.by(owner);
        for (String pattern : entry.patterns()) b.pattern(pattern);
        applyMeta(entry.meta(), b::description, b::example, b::since, b::category, b::deprecated);
        b.injectableHandler(ownerClass, entry.method());
    }

    private static void applyMeta(@NotNull IndexedMeta meta, @NotNull Consumer<String> description, @NotNull Consumer<String> example, @NotNull Consumer<String> since, @NotNull Consumer<Category> category, @NotNull Consumer<Boolean> deprecated) {
        if (meta.description() != null) description.accept(meta.description());
        for (String ex : meta.examples()) example.accept(ex);
        if (meta.since() != null) since.accept(meta.since());
        if (meta.category() != null) category.accept(Categories.createOrGet(meta.category()));
        if (meta.deprecated()) deprecated.accept(true);
    }
}
