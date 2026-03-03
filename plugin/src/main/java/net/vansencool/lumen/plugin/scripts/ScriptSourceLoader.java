package net.vansencool.lumen.plugin.scripts;

import net.vansencool.lumen.pipeline.logger.LumenLogger;
import net.vansencool.lumen.plugin.Lumen;
import net.vansencool.lumen.plugin.configuration.LumenConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ScriptSourceLoader {

    public static Path scriptsDir() {
        return Lumen.instance()
                .getDataFolder()
                .toPath()
                .resolve(LumenConfiguration.SCRIPTS.FOLDER);
    }

    public static String load(String fileName) {
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

    public static List<String> list() {
        Path dir = scriptsDir();
        if (!Files.isDirectory(dir)) return List.of();

        try (var stream = Files.list(dir)) {
            return stream
                    .filter(p -> p.getFileName().toString().endsWith(LumenConfiguration.SCRIPTS.EXTENSION))
                    .filter(p -> !p.getFileName().toString().startsWith("-"))
                    .filter(p -> {
                        String name = p.getFileName().toString();
                        if (isReservedScriptName(name)) {
                            LumenLogger.warning("Skipping reserved script name '" + name
                                    + "'. Names whose base is surrounded by __ are reserved for internal use.");
                            return false;
                        }
                        return true;
                    })
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    /**
     * Returns true if the script file name uses a reserved naming convention.
     *
     * <p>A name is considered reserved when its base part (the file name without
     * extension) starts and ends with double underscores, for example
     * {@code __warmup__.luma} or {@code __internal__.luma}.
     *
     * @param fileName the script file name including its extension
     * @return true if the name is reserved
     */
    private static boolean isReservedScriptName(@NotNull String fileName) {
        int dot = fileName.lastIndexOf('.');
        String base = dot >= 0 ? fileName.substring(0, dot) : fileName;
        return base.startsWith("__") && base.endsWith("__");
    }
}
