package dev.lumenlang.build.emit.write;

import com.google.gson.Gson;
import dev.lumenlang.lumen.api.inject.index.SidecarEntry;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Writes the gzipped Gson sidecar to {@code META-INF/lumen/sources.gson.gz}.
 * The sidecar holds the original Java source text of every annotated handler
 * method, keyed by class internal name + method descriptor.
 */
public final class SourceSidecarWriter {

    private static final String RESOURCE_PATH = "META-INF/lumen/sources.gson.gz";
    private static final Gson GSON = new Gson();

    private SourceSidecarWriter() {
    }

    public static @NotNull Path write(@NotNull Path resourcesDir, @NotNull List<SidecarEntry> entries) throws IOException {
        Path target = resourcesDir.resolve(RESOURCE_PATH);
        Files.createDirectories(target.getParent());
        try (OutputStream raw = Files.newOutputStream(target);
             GZIPOutputStream gz = new GZIPOutputStream(raw);
             Writer writer = new OutputStreamWriter(gz, StandardCharsets.UTF_8)) {
            GSON.toJson(Map.of("handlers", entries), writer);
        }
        return target;
    }
}
