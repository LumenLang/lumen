package dev.lumenlang.lumen.plugin.scripts.cache;

import dev.lumenlang.lumen.pipeline.java.version.JavaVersions;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.plugin.Lumen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Stream;

/**
 * On-disk cache of compiled script bytecodes.
 */
@SuppressWarnings("unused")
public final class CompiledClassCache {

    private CompiledClassCache() {
    }

    /**
     * Reads the cached bundle for {@code scriptName} if its checksum, Java
     * version, and Lumen version all match the current values.
     *
     * @return class name to bytecode bytes, or null on miss
     */
    public static @Nullable Map<String, byte[]> load(@NotNull String scriptName, @NotNull String source) {
        Path dir = cacheDir(scriptName);
        Path meta = dir.resolve("metadata.txt");

        if (!Files.isRegularFile(meta)) {
            LumenLogger.debug("CompiledClassCache", "Cache miss (no meta): " + scriptName);
            return null;
        }

        try {
            String[] lines = Files.readString(meta).split("\n", 3);
            if (lines.length < 3) {
                LumenLogger.debug("CompiledClassCache", "Cache miss (corrupt meta): " + scriptName);
                return null;
            }

            String storedJava = lines[1].trim();
            String storedLumen = lines[2].trim();

            String currentJava = String.valueOf(JavaVersions.current().value());
            String currentLumen = Lumen.instance().getDescription().getVersion();

            if (!checksum(source).equals(lines[0].trim())) {
                LumenLogger.debug("CompiledClassCache", "Cache miss (source changed): " + scriptName);
                return null;
            }

            if (!currentJava.equals(storedJava)) {
                LumenLogger.debug("CompiledClassCache", "Cache miss (java version changed " + storedJava + " to " + currentJava + "): " + scriptName);
                return null;
            }

            if (!currentLumen.equals(storedLumen)) {
                LumenLogger.debug("CompiledClassCache", "Cache miss (lumen version changed " + storedLumen + " to " + currentLumen + "): " + scriptName);
                return null;
            }

            Map<String, byte[]> bytecodes = new HashMap<>();
            try (Stream<Path> entries = Files.list(dir)) {
                for (Path entry : entries.toList()) {
                    String fileName = entry.getFileName().toString();
                    if (!fileName.endsWith(".class")) continue;
                    String className = fileName.substring(0, fileName.length() - ".class".length());
                    bytecodes.put(className.replace('%', '.'), Files.readAllBytes(entry));
                }
            }

            if (bytecodes.isEmpty()) {
                LumenLogger.debug("CompiledClassCache", "Cache miss (no .class files): " + scriptName);
                return null;
            }

            LumenLogger.debug("CompiledClassCache", "Cache hit (" + bytecodes.size() + " classes): " + scriptName);
            return bytecodes;
        } catch (IOException e) {
            LumenLogger.debug("CompiledClassCache", "Cache read error for " + scriptName + ": " + e.getMessage());
            return null;
        }
    }

    public static void save(@NotNull String scriptName, @NotNull String source, @NotNull Map<String, byte[]> bytecodes) {
        Path dir = cacheDir(scriptName);
        if (bytecodes.isEmpty()) {
            LumenLogger.debug("CompiledClassCache", "Not caching " + scriptName + ": bytecodes map is empty");
            return;
        }
        try {
            clearDir(dir);
            Files.createDirectories(dir);

            String meta = checksum(source) + "\n" + JavaVersions.current().value() + "\n" + Lumen.instance().getDescription().getVersion();
            Files.writeString(dir.resolve("metadata.txt"), meta);

            int classCount = 0;
            for (var entry : bytecodes.entrySet()) {
                Files.write(dir.resolve(entry.getKey().replace('.', '%') + ".class"), entry.getValue());
                classCount++;
            }

            LumenLogger.debug("CompiledClassCache", "Saved " + classCount + " classes to " + dir + " for " + scriptName);
        } catch (IOException e) {
            LumenLogger.severe("[CompiledClassCache] Cache write error for " + scriptName + " to " + dir + ": " + e.getMessage(), e);
        }
    }

    public static void saveJavaSource(@NotNull String scriptName, @NotNull String javaSource) {
        Path dir = cacheDir(scriptName);
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("sourcemap.java"), javaSource);
        } catch (IOException e) {
            LumenLogger.debug("CompiledClassCache", "Failed to save java source for " + scriptName + ": " + e.getMessage());
        }
    }

    public static @Nullable String loadJavaSource(@NotNull String scriptName) {
        Path file = cacheDir(scriptName).resolve("sourcemap.java");
        if (!Files.isRegularFile(file)) return null;
        try {
            return Files.readString(file);
        } catch (IOException e) {
            LumenLogger.debug("CompiledClassCache", "Failed to read java source for " + scriptName + ": " + e.getMessage());
            return null;
        }
    }

    public static void invalidate(@NotNull String scriptName) {
        try {
            clearDir(cacheDir(scriptName));
        } catch (IOException e) {
            LumenLogger.debug("CompiledClassCache", "Cache invalidation error for " + scriptName + ": " + e.getMessage());
        }
    }

    public static @NotNull Path compiledRoot() {
        return Lumen.instance().getDataFolder().toPath().resolve("compiled");
    }

    private static @NotNull Path cacheDir(@NotNull String scriptName) {
        String safeName = scriptName.replace('/', '_').replace('\\', '_');
        return compiledRoot().resolve(safeName);
    }

    private static @NotNull String checksum(@NotNull String source) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            try {
                byte[] fallback = MessageDigest.getInstance("MD5").digest(source.getBytes(StandardCharsets.UTF_8));
                LumenLogger.severe("SHA-256 not available, using MD5 for CompiledClassCache checksums");
                return HexFormat.of().formatHex(fallback);
            } catch (NoSuchAlgorithmException e2) {
                throw new RuntimeException("No suitable hashing algorithm available for CompiledClassCache", e2);
            }
        }
    }

    private static void clearDir(@NotNull Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return;
        try (Stream<Path> entries = Files.list(dir)) {
            for (Path entry : entries.toList()) {
                String fileName = entry.getFileName().toString();
                if (fileName.endsWith(".class") || fileName.equals("metadata.txt") || fileName.equals("sourcemap.java")) {
                    Files.deleteIfExists(entry);
                }
            }
        }
    }
}
