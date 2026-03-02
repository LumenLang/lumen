package net.vansencool.lumen.plugin.scripts;

import net.vansencool.lumen.pipeline.logger.LumenLogger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Copies bundled example scripts from the plugin JAR into the
 * {@code examples/} subdirectory inside the configured scripts folder.
 *
 * <p>All {@code .luma} files under {@code /examples/} in the JAR are
 * discovered dynamically, including subdirectories (e.g. {@code examples/small/}).
 * Files are only copied when they do not already exist, so user
 * modifications are preserved across restarts.
 */
public final class ExampleCopier {

    private ExampleCopier() {
    }

    /**
     * Copies all bundled example scripts into the given scripts directory.
     *
     * <p>Walks the {@code /examples/} resource tree inside the plugin JAR
     * and copies every {@code .luma} file into the corresponding path under
     * {@code scriptsDir/examples/}. Existing files are never overwritten.
     *
     * @param scriptsDir the root scripts directory
     */
    public static void copyExamples(@NotNull Path scriptsDir) {
        Path examplesDir = scriptsDir.resolve("examples");
        try {
            Files.createDirectories(examplesDir);
        } catch (IOException e) {
            LumenLogger.warning("Failed to create examples directory: " + e.getMessage());
            return;
        }

        URI jarUri;
        try {
            jarUri = ExampleCopier.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        } catch (URISyntaxException e) {
            LumenLogger.warning("Failed to resolve JAR URI: " + e.getMessage());
            return;
        }

        try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + jarUri), Map.of())) {
            Path examplesRoot = fs.getPath("/examples");
            if (!Files.isDirectory(examplesRoot)) {
                LumenLogger.warning("No /examples/ directory found in JAR.");
                return;
            }

            try (Stream<Path> walk = Files.walk(examplesRoot)) {
                walk.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".luma"))
                        .forEach(jarPath -> copyOne(examplesRoot, jarPath, examplesDir));
            }
        } catch (IOException e) {
            LumenLogger.warning("Failed to walk examples in JAR: " + e.getMessage());
        }
    }

    /**
     * Copies a single example script from the JAR to the target directory,
     * preserving the relative subdirectory structure.
     *
     * @param examplesRoot the root {@code /examples/} path inside the JAR
     * @param jarPath      the full path to the file inside the JAR
     * @param examplesDir  the target examples directory on disk
     */
    private static void copyOne(@NotNull Path examplesRoot, @NotNull Path jarPath,
                                @NotNull Path examplesDir) {
        String relative = examplesRoot.relativize(jarPath).toString();
        Path target = examplesDir.resolve(relative);

        if (Files.exists(target)) return;

        try {
            Files.createDirectories(target.getParent());
        } catch (IOException e) {
            LumenLogger.warning("Failed to create directory for example: " + relative);
            return;
        }

        try (InputStream in = Files.newInputStream(jarPath)) {
            Files.copy(in, target);
            LumenLogger.debug("ExampleCopier", "Copied example script: " + relative);
        } catch (IOException e) {
            LumenLogger.warning("Failed to copy example script " + relative + ": " + e.getMessage());
        }
    }
}
