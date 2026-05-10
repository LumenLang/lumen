package dev.lumenlang.build.validate.inject;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Loads {@link BindingTypeTable} entries from a Lumen {@code .ldoc} file. The
 * doc bundles every registered binding's {@code javaType} (Java source-level
 * name); this loader converts each into a JVMS field descriptor for
 * {@link InjectTypeValidator}.
 */
public final class BindingTableLoader {

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final Gson GSON = new Gson();

    private BindingTableLoader() {
    }

    /**
     * Downloads a gzipped {@code .ldoc} from {@code url} and caches the
     * decompressed JSON under {@code cacheDir}. Reuses cache on subsequent
     * calls.
     */
    public static @NotNull BindingTypeTable loadFromUrl(@NotNull String url, @NotNull Path cacheDir) throws IOException, InterruptedException {
        Files.createDirectories(cacheDir);
        Path cached = cacheDir.resolve(url.replaceAll("[^A-Za-z0-9.-]", "_") + ".json");
        if (!Files.isRegularFile(cached)) {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().timeout(Duration.ofSeconds(30)).build();
            HttpResponse<byte[]> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() != 200) {
                throw new IOException("Failed to download " + url + ": HTTP " + resp.statusCode());
            }
            String json = decompress(resp.body());
            Files.writeString(cached, json, StandardCharsets.UTF_8);
        }
        return parse(Files.readString(cached, StandardCharsets.UTF_8));
    }

    /**
     * Loads from a local gzipped {@code .ldoc} file.
     */
    public static @NotNull BindingTypeTable loadFromFile(@NotNull Path file) throws IOException {
        return parse(decompress(Files.readAllBytes(file)));
    }

    private static @NotNull String decompress(byte @NotNull [] gz) throws IOException {
        try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(gz))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Parses an {@code .ldoc} JSON body and returns the binding table.
     */
    public static @NotNull BindingTypeTable parse(@NotNull String json) {
        Map<String, String> bindings = new HashMap<>();
        JsonObject root = GSON.fromJson(json, JsonObject.class);
        if (root == null) return new BindingTypeTable(Map.of());
        JsonElement typeBindings = root.get("typeBindings");
        if (typeBindings == null || !typeBindings.isJsonArray()) return new BindingTypeTable(Map.of());
        for (JsonElement e : typeBindings.getAsJsonArray()) {
            if (!e.isJsonObject()) continue;
            JsonObject obj = e.getAsJsonObject();
            String id = stringOrNull(obj, "id");
            String javaType = stringOrNull(obj, "javaType");
            if (id == null || javaType == null) continue;
            bindings.put(id, javaTypeToDescriptor(javaType));
        }
        return new BindingTypeTable(Map.copyOf(bindings));
    }

    private static @NotNull String javaTypeToDescriptor(@NotNull String javaType) {
        String type = javaType.trim();
        int arrayDims = 0;
        while (type.endsWith("[]")) {
            arrayDims++;
            type = type.substring(0, type.length() - 2).trim();
        }
        if ("String".equals(type)) type = "java.lang.String";
        String base = switch (type) {
            case "byte" -> "B";
            case "short" -> "S";
            case "char" -> "C";
            case "int" -> "I";
            case "long" -> "J";
            case "float" -> "F";
            case "double" -> "D";
            case "boolean" -> "Z";
            case "void" -> "V";
            default -> "L" + type.replace('.', '/') + ";";
        };
        return "[".repeat(arrayDims) + base;
    }

    private static String stringOrNull(@NotNull JsonObject obj, @NotNull String key) {
        JsonElement el = obj.get(key);
        return el == null || el.isJsonNull() ? null : el.getAsString();
    }

}
