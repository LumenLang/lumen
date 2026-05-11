package dev.lumenlang.lumen.pipeline.inject.loader;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.lumenlang.lumen.api.inject.index.IndexedHandler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads {@code META-INF/lumen/handlers.json} from a {@link ClassLoader} and
 * returns the handler entries the build plugin emitted at addon-build time.
 */
public final class IndexLoader {

    private static final String RESOURCE_PATH = "META-INF/lumen/handlers.json";
    private static final Gson GSON = new Gson();
    private static final Type ENVELOPE_TYPE = new TypeToken<Map<String, List<IndexedHandler>>>() {}.getType();

    private static final Set<String> SEEN_URLS = Collections.synchronizedSet(new HashSet<>());

    private IndexLoader() {
    }

    public static @NotNull List<IndexedHandler> load(@NotNull ClassLoader loader) throws IOException {
        List<IndexedHandler> all = new ArrayList<>();
        Enumeration<URL> urls = loader.getResources(RESOURCE_PATH);
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            if (!SEEN_URLS.add(url.toString())) continue;
            try (InputStream in = url.openStream();
                 InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                Map<String, List<IndexedHandler>> envelope = GSON.fromJson(reader, ENVELOPE_TYPE);
                if (envelope == null) continue;
                List<IndexedHandler> handlers = envelope.get("handlers");
                if (handlers != null) all.addAll(handlers);
            }
        }
        return all;
    }
}
