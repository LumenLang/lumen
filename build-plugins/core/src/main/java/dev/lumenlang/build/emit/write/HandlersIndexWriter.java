package dev.lumenlang.build.emit.write;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.lumenlang.lumen.api.inject.index.IndexedHandler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Writes the human-readable handlers index to {@code META-INF/lumen/handlers.json}
 * inside the addon's resources directory.
 */
public final class HandlersIndexWriter {

    private static final String RESOURCE_PATH = "META-INF/lumen/handlers.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private HandlersIndexWriter() {
    }

    public static @NotNull Path write(@NotNull Path resourcesDir, @NotNull List<IndexedHandler> handlers) throws IOException {
        Path target = resourcesDir.resolve(RESOURCE_PATH);
        Files.createDirectories(target.getParent());
        String json = GSON.toJson(Map.of("handlers", handlers));
        Files.writeString(target, json, StandardCharsets.UTF_8);
        return target;
    }
}
