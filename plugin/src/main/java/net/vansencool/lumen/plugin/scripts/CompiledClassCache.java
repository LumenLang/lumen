package net.vansencool.lumen.plugin.scripts;

import net.vansencool.lumen.pipeline.java.version.JavaVersions;
import net.vansencool.lumen.pipeline.logger.LumenLogger;
import net.vansencool.lumen.plugin.Lumen;
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
 * Manages a persistent on-disk cache of compiled script bytecodes.
 *
 * <p>
 * Each script gets its own subdirectory under
 * {@code <dataFolder>/compiled/<scriptName>/}.
 * That directory contains:
 * <ul>
 * <li>{@code metadata.txt} - a text file recording the SHA-256 of the script
 * source and the
 * Java specification version at compile time, separated by a newline.</li>
 * <li>{@code <ClassName>.class} - one raw bytecode file per compiled class
 * (including inner
 * classes produced by the compiler).</li>
 * </ul>
 *
 * <p>
 * A cache entry is considered a hit only when both the source checksum and the
 * Java version
 * match the values recorded in the {@code metadata.txt} file. Either changing
 * the script source or
 * running under a different JVM version invalidates the cache and triggers a
 * fresh compile.
 *
 * @see ScriptManager
 */
@SuppressWarnings("unused")
public final class CompiledClassCache {

    private CompiledClassCache() {
    }

    /**
     * Attempts to load cached bytecodes for a script.
     *
     * <p>
     * Returns {@code null} when:
     * <ul>
     * <li>caching is disabled via {@code performance.cache-compiled-classes}</li>
     * <li>no cache directory exists for the script</li>
     * <li>the stored checksum does not match the current source</li>
     * <li>the stored Java version does not match the running JVM</li>
     * <li>any I/O error occurs reading the cache</li>
     * </ul>
     *
     * @param scriptName the file name of the script (e.g. {@code "hello.luma"})
     * @param source     the current source text of the script
     * @return a map of fully-qualified class name → bytecodes, or {@code null} on a
     *         cache miss
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
                LumenLogger.debug("CompiledClassCache",
                        "Cache miss (java version changed " + storedJava + " to " + currentJava + "): " + scriptName);
                return null;
            }

            if (!currentLumen.equals(storedLumen)) {
                LumenLogger.debug("CompiledClassCache", "Cache miss (lumen version changed " + storedLumen + " to "
                        + currentLumen + "): " + scriptName);
                return null;
            }

            Map<String, byte[]> bytecodes = new HashMap<>();
            try (Stream<Path> entries = Files.list(dir)) {
                for (Path entry : entries.toList()) {
                    String fileName = entry.getFileName().toString();
                    if (!fileName.endsWith(".class"))
                        continue;
                    String className = fileName.substring(0, fileName.length() - ".class".length());
                    bytecodes.put(decodeClassName(className), Files.readAllBytes(entry));
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

    /**
     * Persists compiled bytecodes for a script to disk.
     *
     * <p>
     * Writes the {@code metadata.txt} file (checksum + Java version) and one
     * {@code .class} file per
     * entry in {@code bytecodes}. Any existing cache directory for this script is
     * cleared first.
     * When an I/O error occurs, the failure is logged at debug level and silently
     * ignored so that
     * a cache write error never prevents the script from running.
     *
     * @param scriptName the file name of the script (e.g. {@code "hello.luma"})
     * @param source     the source text that was compiled
     * @param bytecodes  the full map of class name → bytecodes returned by the
     *                   compiler
     */
    public static void save(@NotNull String scriptName, @NotNull String source,
            @NotNull Map<String, byte[]> bytecodes) {
        Path dir = cacheDir(scriptName);
        if (bytecodes.isEmpty()) {
            LumenLogger.debug("CompiledClassCache", "Not caching " + scriptName + ": bytecodes map is empty");
            return;
        }
        try {
            clearDir(dir);
            Files.createDirectories(dir);

            String meta = checksum(source) + "\n" + JavaVersions.current().value() + "\n" + Lumen.instance().getDescription().getVersion();
            Path metaFile = dir.resolve("metadata.txt");
            Files.writeString(metaFile, meta);

            int classCount = 0;
            for (var entry : bytecodes.entrySet()) {
                String fileName = encodeClassName(entry.getKey()) + ".class";
                Files.write(dir.resolve(fileName), entry.getValue());
                classCount++;
            }

            LumenLogger.debug("CompiledClassCache",
                    "Saved " + classCount + " classes to " + dir + " for " + scriptName);
        } catch (IOException e) {
            LumenLogger.severe(
                    "[CompiledClassCache] Cache write error for " + scriptName + " to " + dir + ": " + e.getMessage(),
                    e);
        }
    }

    /**
     * Persists the generated Java source for a script alongside its compiled
     * bytecodes so that {@link net.vansencool.lumen.pipeline.java.compiled.ScriptSourceMap}
     * can be restored when the script is loaded from cache on subsequent server
     * starts.
     *
     * <p>
     * Failures are logged at debug level and silently ignored.
     *
     * @param scriptName the file name of the script (e.g. {@code "hello.luma"})
     * @param javaSource the generated Java source produced by the code emitter
     */
    public static void saveJavaSource(@NotNull String scriptName, @NotNull String javaSource) {
        Path dir = cacheDir(scriptName);
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("sourcemap.java"), javaSource);
        } catch (IOException e) {
            LumenLogger.debug("CompiledClassCache", "Failed to save java source for " + scriptName + ": " + e.getMessage());
        }
    }

    /**
     * Loads the previously saved generated Java source for a script.
     *
     * @param scriptName the file name of the script (e.g. {@code "hello.luma"})
     * @return the generated Java source, or {@code null} if not present or unreadable
     */
    public static @Nullable String loadJavaSource(@NotNull String scriptName) {
        Path file = cacheDir(scriptName).resolve("sourcemap.java");
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            return Files.readString(file);
        } catch (IOException e) {
            LumenLogger.debug("CompiledClassCache", "Failed to read java source for " + scriptName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Deletes the cache directory for the given script, if it exists.
     *
     * @param scriptName the file name of the script
     */
    public static void invalidate(@NotNull String scriptName) {
        try {
            clearDir(cacheDir(scriptName));
        } catch (IOException e) {
            LumenLogger.debug("CompiledClassCache",
                    "Cache invalidation error for " + scriptName + ": " + e.getMessage());
        }
    }

    /**
     * Returns the {@code <dataFolder>/compiled} directory root.
     *
     * @return the compiled cache root path
     */
    public static @NotNull Path compiledRoot() {
        return Lumen.instance().getDataFolder().toPath().resolve("compiled");
    }

    private static @NotNull Path cacheDir(@NotNull String scriptName) {
        String safeName = scriptName.replace('/', '_').replace('\\', '_');
        return compiledRoot().resolve(safeName);
    }

    private static @NotNull String checksum(@NotNull String source) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            try {
                byte[] fallback = MessageDigest.getInstance("MD5").digest(source.getBytes(StandardCharsets.UTF_8));
                LumenLogger.severe("SHA-256 not available, using MD5 for CompiledClassCache checksums");
                return HexFormat.of().formatHex(fallback);
            } catch (NoSuchAlgorithmException e2) {
                // This should never happen
                throw new RuntimeException("No suitable hashing algorithm available for CompiledClassCache", e2);
            }
        }
    }

    /**
     * Encodes a fully-qualified class name so it can be used as a safe filename.
     * Dots are replaced with {@code %} to avoid clashing with the file extension.
     */
    private static @NotNull String encodeClassName(@NotNull String className) {
        return className.replace('.', '%');
    }

    /**
     * Decodes a filename produced by {@link #encodeClassName(String)} back to a
     * class name.
     */
    private static @NotNull String decodeClassName(@NotNull String encoded) {
        return encoded.replace('%', '.');
    }

    private static void clearDir(@NotNull Path dir) throws IOException {
        if (!Files.isDirectory(dir))
            return;
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
