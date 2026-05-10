package dev.lumenlang.lumen.plugin.scripts.source;

import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.plugin.Lumen;
import dev.lumenlang.lumen.plugin.configuration.LumenConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Reads script files from the configured scripts directory.
 */
public final class ScriptSourceLoader {

    private ScriptSourceLoader() {
    }

    public static @NotNull Path scriptsDir() {
        return Lumen.instance().getDataFolder().toPath().resolve(LumenConfiguration.SCRIPTS.FOLDER);
    }

    public static @NotNull String load(@NotNull String fileName) {
        Path p = scriptsDir().resolve(fileName);
        if (!Files.exists(p)) {
            throw new IllegalArgumentException("Script file " + fileName + " does not exist");
        }
        try {
            return Files.readString(p);
        } catch (IOException e) {
            throw new RuntimeException("Cannot load script " + fileName + ": " + e.getMessage());
        }
    }

    /**
     * Recursively lists every script file under the scripts directory. Returns
     * paths relative to the directory. Hidden ({@code -name.luma}) and reserved
     * ({@code __name__.luma}) files are skipped, as is any file inside a hidden
     * parent ({@code -folder/...}).
     */
    public static @NotNull List<String> list() {
        Path dir = scriptsDir();
        if (!Files.isDirectory(dir)) return List.of();

        String extension = LumenConfiguration.SCRIPTS.EXTENSION;
        List<String> result = new ArrayList<>();

        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(Files::isRegularFile).forEach(p -> {
                Path relative = dir.relativize(p);
                String fileName = p.getFileName().toString();

                if (!fileName.endsWith(extension)) return;
                for (Path part : relative) {
                    if (part.toString().startsWith("-")) return;
                }

                int dot = fileName.lastIndexOf('.');
                String base = dot >= 0 ? fileName.substring(0, dot) : fileName;
                if (base.startsWith("__") && base.endsWith("__")) {
                    LumenLogger.warning("Skipping reserved script name '" + relative + "'. Names whose base is wrapped in __ are reserved.");
                    return;
                }

                result.add(relative.toString().replace('\\', '/'));
            });
        } catch (IOException e) {
            return List.of();
        }

        result.sort(Comparator.naturalOrder());
        return result;
    }
}
