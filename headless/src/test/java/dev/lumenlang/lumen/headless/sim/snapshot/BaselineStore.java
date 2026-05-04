package dev.lumenlang.lumen.headless.sim.snapshot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Reads and writes per-case baseline files under {@code headless/snapshots}, resolved relative to
 * the test JVM's working directory (the headless module root).
 */
public final class BaselineStore {

    private static final @NotNull Path ROOT = Paths.get("snapshots");

    private BaselineStore() {
    }

    /**
     * Resolved path for {@code caseName}, with characters unsafe for filenames replaced.
     */
    public static @NotNull Path pathFor(@NotNull String caseName) {
        return ROOT.resolve(safeName(caseName) + ".snap");
    }

    /**
     * Reads the stored baseline, or {@code null} when no baseline exists yet.
     */
    public static @Nullable Snapshot read(@NotNull String caseName) {
        Path path = pathFor(caseName);
        if (!Files.exists(path)) return null;
        try {
            return SnapshotSerializer.deserialize(Files.readString(path));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read baseline " + path, e);
        }
    }

    /**
     * Writes {@code snap} to disk, creating parent directories as needed.
     */
    public static void write(@NotNull Snapshot snap) {
        Path path = pathFor(snap.caseName());
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, SnapshotSerializer.serialize(snap));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write baseline " + path, e);
        }
    }

    private static @NotNull String safeName(@NotNull String name) {
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '-' || c == '.') {
                sb.append(c);
            } else if (c == ' ') {
                sb.append('_');
            } else {
                sb.append('-');
            }
        }
        return sb.toString();
    }
}
