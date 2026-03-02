package net.vansencool.lumen.pipeline.persist.impl;

import net.vansencool.lumen.pipeline.logger.LumenLogger;
import net.vansencool.lumen.pipeline.persist.PersistentStorage;
import net.vansencool.lumen.pipeline.persist.serializer.LumenSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Default file-based implementation of {@link PersistentStorage}.
 *
 * <p>Data is stored in a single binary file. Each entry is serialized as a key-value pair
 * where the key is a UTF-8 string and the value is delegated entirely to
 * {@link LumenSerializer} for encoding/decoding. This means any type the serializer
 * supports is automatically stored correctly without changes here.
 *
 * <p>The entire file is read into memory on {@link #load()}. Writes are debounced and
 * flushed asynchronously after a short delay to avoid excessive disk I/O on rapid
 * successive updates. A synchronous {@link #flush()} is still called during shutdown
 * to ensure all data is persisted.
 *
 * @see LumenSerializer
 */
public final class FilePersistentStorage implements PersistentStorage {

    private static final long FLUSH_DELAY_MS = 500;

    private final Path file;
    private final Map<String, Object> data = new ConcurrentHashMap<>();
    private final LumenSerializer serializer = LumenSerializer.instance();
    private final ScheduledExecutorService flushExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Lumen-PersistFlush");
        t.setDaemon(true);
        return t;
    });
    private volatile ScheduledFuture<?> pendingFlush;

    /**
     * Creates a new file-based storage at the given path.
     *
     * @param file the path to the storage file
     */
    public FilePersistentStorage(@NotNull Path file) {
        this.file = file;
    }

    @Override
    public @Nullable Object get(@NotNull String key) {
        return data.get(key);
    }

    @Override
    public void set(@NotNull String key, @NotNull Object value) {
        data.put(key, value);
        scheduleFlush();
    }

    @Override
    public void delete(@NotNull String key) {
        data.remove(key);
        scheduleFlush();
    }

    @Override
    public void deleteByPrefix(@NotNull String prefix) {
        data.keySet().removeIf(k -> k.startsWith(prefix));
        scheduleFlush();
    }

    private void scheduleFlush() {
        ScheduledFuture<?> existing = pendingFlush;
        if (existing != null && !existing.isDone()) existing.cancel(false);
        pendingFlush = flushExecutor.schedule(this::doFlush, FLUSH_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void flush() {
        ScheduledFuture<?> existing = pendingFlush;
        if (existing != null) existing.cancel(false);
        doFlush();
        flushExecutor.shutdown();
    }

    private synchronized void doFlush() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeInt(data.size());
            for (var entry : data.entrySet()) {
                dos.writeUTF(entry.getKey());
                byte[] valueBytes = serializer.serialize(entry.getValue());
                dos.writeInt(valueBytes.length);
                dos.write(valueBytes);
            }
            dos.flush();

            Files.createDirectories(file.getParent());
            Files.write(file, baos.toByteArray());
        } catch (IOException e) {
            LumenLogger.severe("Failed to save persistent variables", e);
        }
    }

    @Override
    public void load() {
        if (!Files.exists(file)) return;

        try {
            byte[] bytes = Files.readAllBytes(file);
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));

            int count = dis.readInt();
            for (int i = 0; i < count; i++) {
                String key = dis.readUTF();
                int len = dis.readInt();
                byte[] valueBytes = new byte[len];
                dis.readFully(valueBytes);
                try {
                    Object value = serializer.deserialize(valueBytes);
                    if (value != null) {
                        data.put(key, value);
                    }
                } catch (Exception e) {
                    LumenLogger.warning("Skipping stored variable '" + key
                            + "': stored data format may have changed. "
                            + "This entry will be removed on next save.");
                }
            }
        } catch (IOException e) {
            LumenLogger.severe("Failed to load persistent variables. "
                    + "The storage file may be corrupted or from an incompatible version.");
        }
    }
}
