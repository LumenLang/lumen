package dev.lumenlang.lumen.debug.auth.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Disk-backed registry of clients the user has approved for debug sessions.
 */
public final class TrustStore {

    private static final Logger LOG = Logger.getLogger("LumenDebug-Trust");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final @NotNull Path file;
    private final @NotNull Map<String, TrustedClient> entries = new ConcurrentHashMap<>();

    public TrustStore(@NotNull Path file) {
        this.file = file;
        load();
    }

    public @Nullable TrustedClient byId(@NotNull String clientId) {
        return entries.get(clientId);
    }

    public void put(@NotNull TrustedClient client) {
        entries.put(client.clientId(), client);
        save();
    }

    public boolean revoke(@NotNull String clientId) {
        TrustedClient removed = entries.remove(clientId);
        if (removed != null) save();
        return removed != null;
    }

    public @NotNull List<TrustedClient> all() {
        return new ArrayList<>(entries.values());
    }

    private void load() {
        if (!Files.exists(file)) return;
        try {
            String json = Files.readString(file);
            if (json.isBlank()) return;
            JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
            for (var el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String id = obj.get("clientId").getAsString();
                String name = obj.get("clientName").getAsString();
                long approvedAt = obj.get("approvedAt").getAsLong();
                TrustScope scope = TrustScope.valueOf(obj.get("scope").getAsString());
                entries.put(id, new TrustedClient(id, name, approvedAt, scope));
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to load debug trust store; starting empty", e);
        }
    }

    private synchronized void save() {
        try {
            Files.createDirectories(file.getParent());
            JsonArray arr = new JsonArray();
            for (TrustedClient c : entries.values()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("clientId", c.clientId());
                obj.addProperty("clientName", c.clientName());
                obj.addProperty("approvedAt", c.approvedAt());
                obj.addProperty("scope", c.scope().name());
                arr.add(obj);
            }
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(arr));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to persist debug trust store", e);
        }
    }
}
