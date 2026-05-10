package dev.lumenlang.lumen.plugin.scripts.pipeline;

import dev.lumenlang.lumen.pipeline.java.compiled.ClassBuilder;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.plugin.scripts.cache.CompiledClassCache;
import dev.lumenlang.lumen.plugin.scripts.model.source.GeneratedSource;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes generated Java sources to disk for inspection.
 */
public final class BytecodeDump {

    private BytecodeDump() {
    }

    /**
     * Writes the source under {@code <compiled>/<script>/dump/}.
     * Internal scripts ({@code __name__}) are skipped.
     */
    public static void dump(@NotNull GeneratedSource source) {
        if (source.scriptName().startsWith("__")) return;

        String safeName = source.scriptName().replace('/', '_').replace('\\', '_');
        Path dumpDir = CompiledClassCache.compiledRoot().resolve(safeName).resolve("dump");
        try {
            Files.createDirectories(dumpDir);
            Files.writeString(dumpDir.resolve(ClassBuilder.normalize(source.className()) + ".java"), source.javaSource());
        } catch (IOException e) {
            LumenLogger.severe("Failed to dump generated Java for " + source.scriptName() + ": " + e.getMessage());
        }
    }
}
