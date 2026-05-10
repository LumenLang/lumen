package dev.lumenlang.lumen.pipeline.inject.loader;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.lumenlang.lumen.api.inject.index.SidecarEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

/**
 * Reads {@code META-INF/lumen/sources.gson.gz} from a {@link ClassLoader}
 * and exposes preserved handler source text by class internal name and
 * method descriptor.
 */
public final class SidecarReader {

    private static final String RESOURCE_PATH = "META-INF/lumen/sources.gson.gz";
    private static final Gson GSON = new Gson();
    private static final Type ENVELOPE_TYPE = new TypeToken<Map<String, List<SidecarEntry>>>() {}.getType();

    private static final Map<String, SidecarEntry> ENTRIES = new ConcurrentHashMap<>();

    private SidecarReader() {
    }

    /**
     * Loads every sidecar entry visible to {@code loader} into the runtime
     * store. Safe to call multiple times: later entries overwrite earlier
     * ones with the same key.
     */
    public static void load(@NotNull ClassLoader loader) throws IOException {
        Enumeration<URL> urls = loader.getResources(RESOURCE_PATH);
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            try (InputStream raw = url.openStream();
                 GZIPInputStream gz = new GZIPInputStream(raw);
                 InputStreamReader reader = new InputStreamReader(gz, StandardCharsets.UTF_8)) {
                Map<String, List<SidecarEntry>> envelope = GSON.fromJson(reader, ENVELOPE_TYPE);
                if (envelope == null) continue;
                List<SidecarEntry> entries = envelope.get("handlers");
                if (entries == null) continue;
                for (SidecarEntry e : entries) ENTRIES.put(key(e.owner(), e.method(), e.descriptor()), e);
            }
        }
    }

    public static @Nullable SidecarEntry find(@NotNull String ownerInternal, @NotNull String methodName, @NotNull String descriptor) {
        return ENTRIES.get(key(ownerInternal, methodName, descriptor));
    }

    private static @NotNull String key(@NotNull String ownerInternal, @NotNull String methodName, @NotNull String descriptor) {
        return ownerInternal + "#" + methodName + descriptor;
    }
}
