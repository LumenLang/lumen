package dev.lumenlang.lumen.plugin.scripts.pipeline;

import dev.lumenlang.lumen.pipeline.java.compiled.ClassBuilder;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.plugin.compiler.ScriptCompiler;
import dev.lumenlang.lumen.plugin.compiler.system.CompilationFailedException;
import dev.lumenlang.lumen.plugin.compiler.system.SourceFile;
import dev.lumenlang.lumen.plugin.configuration.LumenConfiguration;
import dev.lumenlang.lumen.plugin.inject.bytecode.BytecodeInjector;
import dev.lumenlang.lumen.plugin.inject.bytecode.InjectableRegistry;
import dev.lumenlang.lumen.plugin.scripts.cache.CompiledClassCache;
import dev.lumenlang.lumen.plugin.scripts.model.compiled.CompileTimings;
import dev.lumenlang.lumen.plugin.scripts.model.compiled.PreparedScript;
import dev.lumenlang.lumen.plugin.scripts.model.source.GeneratedSource;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compiles emitted Java sources into bytecode, with batch and per-script paths.
 */
public final class ScriptCompilation {

    private ScriptCompilation() {
    }

    /**
     * Compiles a single script. Throws on compile failure with errors logged.
     */
    public static @NotNull PreparedScript compileOne(@NotNull GeneratedSource source, long parseNanos) {
        long compileStart = System.nanoTime();
        Map<String, byte[]> bytecodes;
        try {
            bytecodes = ScriptCompiler.compileAll(List.of(new SourceFile(source.fqcn(), source.javaSource())));
        } catch (CompilationFailedException e) {
            BytecodeDump.dump(source);
            CompileErrorLog.logForScript(e.errors(), source.scriptName());
            StringBuilder msg = new StringBuilder("Compilation failed for script: ").append(source.scriptName()).append("\n");
            for (CompilationFailedException.CompileError err : e.errors()) {
                msg.append("Line ").append(err.javaLine()).append(": ").append(err.message()).append("\n");
            }
            InjectableRegistry.clear(source.fqcn());
            throw new RuntimeException(msg.toString(), e);
        } catch (Exception e) {
            BytecodeDump.dump(source);
            LumenLogger.severe("[Script " + source.scriptName() + "] Unexpected compiler error: " + e.getMessage());
            InjectableRegistry.clear(source.fqcn());
            throw new RuntimeException("Compilation failed for script: " + source.scriptName(), e);
        }

        finishOne(source, bytecodes);
        long compileNanos = System.nanoTime() - compileStart;

        if (LumenConfiguration.DEBUG.LOG_COMPILATION) {
            LumenLogger.info("[Compilation] Compiled " + source.scriptName() + " in " + String.format("%.3f", compileNanos / 1_000_000.0) + " ms");
        }

        return new PreparedScript(source.scriptName(), source.fqcn(), bytecodes, new CompileTimings(parseNanos, compileNanos));
    }

    /**
     * Compiles many scripts together; falls back to one-by-one on batch failure.
     */
    public static @NotNull List<PreparedScript> compileBatch(@NotNull List<GeneratedSource> generated, long parseTotalNanos) {
        List<SourceFile> files = new ArrayList<>();
        for (GeneratedSource s : generated) {
            files.add(new SourceFile(s.fqcn(), s.javaSource()));
        }

        long compileStart = System.nanoTime();
        Map<String, byte[]> allClasses;
        try {
            allClasses = ScriptCompiler.compileAll(files);
        } catch (CompilationFailedException e) {
            CompileErrorLog.logForBatch(e.errors(), generated);
            LumenLogger.severe("Batch compilation failed due to script errors, falling back to individual compilation...");
            return individualFallback(generated);
        } catch (Exception e) {
            LumenLogger.severe("Batch compilation failed (" + e.getMessage() + "), falling back to individual compilation...");
            return individualFallback(generated);
        }

        long compileTotal = System.nanoTime() - compileStart;
        long parsePerScript = generated.isEmpty() ? 0 : parseTotalNanos / generated.size();
        long compilePerScript = generated.isEmpty() ? 0 : compileTotal / generated.size();

        List<PreparedScript> result = new ArrayList<>();
        for (GeneratedSource s : generated) {
            Map<String, byte[]> bytecodes = extractBytecodes(allClasses, ClassBuilder.normalize(s.className()));
            finishOne(s, bytecodes);
            result.add(new PreparedScript(s.scriptName(), s.fqcn(), bytecodes, new CompileTimings(parsePerScript, compilePerScript)));
        }

        double compileAvgMs = compileTotal / (double) generated.size() / 1_000_000.0;
        LumenLogger.info("Compiled " + generated.size() + " scripts in " + String.format("%.6f", compileTotal / 1_000_000.0) + " ms | compile(avg)=" + String.format("%.6f", compileAvgMs) + " ms");

        return result;
    }

    private static @NotNull List<PreparedScript> individualFallback(@NotNull List<GeneratedSource> generated) {
        List<PreparedScript> result = new ArrayList<>();
        for (GeneratedSource gs : generated) {
            try {
                result.add(compileOne(gs, 0L));
            } catch (RuntimeException ignored) {
            }
        }
        return result;
    }

    private static void finishOne(@NotNull GeneratedSource source, @NotNull Map<String, byte[]> bytecodes) {
        BytecodeInjector.inject(bytecodes);
        boolean internal = source.scriptName().startsWith("__");
        if (!internal) BytecodeDump.dump(source);
        if (!internal && LumenConfiguration.PERFORMANCE.CACHE_COMPILED_CLASSES) {
            CompiledClassCache.save(source.scriptName(), source.originalSource(), bytecodes);
            CompiledClassCache.saveJavaSource(source.scriptName(), source.javaSource());
        }
    }

    private static @NotNull Map<String, byte[]> extractBytecodes(@NotNull Map<String, byte[]> allClasses, @NotNull String normalizedName) {
        String prefix = ClassBuilder.PACKAGE + "." + normalizedName;
        Map<String, byte[]> result = new HashMap<>();
        for (var entry : allClasses.entrySet()) {
            String name = entry.getKey();
            if (name.equals(prefix) || name.startsWith(prefix + "$")) {
                result.put(name, entry.getValue());
            }
        }

        if (result.isEmpty() && !allClasses.isEmpty()) {
            LumenLogger.severe("[CompiledClassCache] Failed to extract bytecodes! Looking for prefix: " + prefix);
            LumenLogger.severe("[CompiledClassCache] Available classes: " + allClasses.keySet());
        }

        return result;
    }
}
