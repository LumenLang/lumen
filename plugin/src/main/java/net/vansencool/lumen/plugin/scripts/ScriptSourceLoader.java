package net.vansencool.lumen.plugin.scripts;

import net.vansencool.lumen.plugin.Lumen;
import net.vansencool.lumen.plugin.configuration.LumenConfiguration;

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
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }
}
