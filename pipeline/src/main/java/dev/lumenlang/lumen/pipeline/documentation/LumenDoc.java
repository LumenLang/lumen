package dev.lumenlang.lumen.pipeline.documentation;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utility for reading and writing Lumen documentation files ({@code .ldoc}).
 * <p>
 * The current format is a GZIP-compressed JSON string, but this may evolve in the future.
 */
public final class LumenDoc {

    public static final String EXTENSION = ".ldoc";

    private LumenDoc() {
    }

    /**
     * Returns the expected resource name for the given addon's documentation file.
     *
     * @param addonName the addon name
     * @return the resource name
     */
    public static @NotNull String resourceName(@NotNull String addonName) {
        return addonName + "-documentation" + EXTENSION;
    }

    /**
     * Writes the given documentation to the specified path.
     *
     * @param path the output file path
     * @param json the JSON content to write
     * @throws IOException if an I/O error occurs
     */
    public static void write(@NotNull Path path, @NotNull String json) throws IOException {
        try (OutputStream os = Files.newOutputStream(path);
             GZIPOutputStream gzos = new GZIPOutputStream(os)) {
            gzos.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Reads a documentation file from the given path.
     *
     * @param path the file path to read
     * @return the decompressed JSON string
     * @throws IOException if an I/O error occurs
     */
    public static @NotNull String readCompressed(@NotNull Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path);
             GZIPInputStream gzis = new GZIPInputStream(is)) {
            return new String(gzis.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Reads a documentation file from the given input stream.
     *
     * @param inputStream the input stream to read from
     * @return the decompressed JSON string
     * @throws IOException if an I/O error occurs
     */
    public static @NotNull String readCompressed(@NotNull InputStream inputStream) throws IOException {
        try (GZIPInputStream gzis = new GZIPInputStream(inputStream)) {
            return new String(gzis.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
