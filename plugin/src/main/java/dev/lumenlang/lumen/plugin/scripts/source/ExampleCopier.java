package dev.lumenlang.lumen.plugin.scripts.source;

import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
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
 * Copies bundled examples from the JAR into {@code <scripts>/-examples/}.
 */
public final class ExampleCopier {

    private ExampleCopier() {
    }

    public static void copyExamples(@NotNull Path scriptsDir) {
        Path examplesDir = scriptsDir.resolve("-examples");
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

    private static void copyOne(@NotNull Path examplesRoot, @NotNull Path jarPath, @NotNull Path examplesDir) {
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
