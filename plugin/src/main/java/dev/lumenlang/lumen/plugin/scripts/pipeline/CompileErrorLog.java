package dev.lumenlang.lumen.plugin.scripts.pipeline;

import dev.lumenlang.lumen.pipeline.java.compiled.ScriptSourceMap;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.plugin.compiler.system.CompilationFailedException;
import dev.lumenlang.lumen.plugin.scripts.model.source.GeneratedSource;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

/**
 * Logs Java-compile errors with their original Lumen source line.
 */
public final class CompileErrorLog {

    private CompileErrorLog() {
    }

    public static void logForScript(@NotNull List<CompilationFailedException.CompileError> errors, @NotNull String scriptName) {
        log(errors, fqcn -> scriptName);
    }

    public static void logForBatch(@NotNull List<CompilationFailedException.CompileError> errors, @NotNull List<GeneratedSource> generated) {
        log(errors, fqcn -> generated.stream()
                .filter(s -> s.fqcn().equals(fqcn))
                .map(GeneratedSource::scriptName)
                .findFirst()
                .orElse(fqcn));
    }

    private static void log(@NotNull List<CompilationFailedException.CompileError> errors, @NotNull Function<String, String> scriptNameOf) {
        for (CompilationFailedException.CompileError err : errors) {
            String scriptName = scriptNameOf.apply(err.fqcn());
            ScriptSourceMap.ScriptLineMapping mapping = err.javaLine() > 0 ? ScriptSourceMap.findScriptLine(err.fqcn(), (int) err.javaLine()) : null;
            if (mapping != null) {
                LumenLogger.severe("[Script " + scriptName + "] Line " + mapping.scriptLine() + ": " + mapping.scriptSource() + " -- " + err.message());
            } else {
                LumenLogger.severe("[Script " + scriptName + "] Compilation error: " + err.message());
            }
        }
        LumenLogger.severe("Got " + errors.size() + " compilation error(s).");
    }
}
