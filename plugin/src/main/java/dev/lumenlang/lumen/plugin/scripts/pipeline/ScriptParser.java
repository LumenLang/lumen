package dev.lumenlang.lumen.plugin.scripts.pipeline;

import dev.lumenlang.lumen.pipeline.codegen.CodegenContextImpl;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnvImpl;
import dev.lumenlang.lumen.pipeline.java.JavaBuilder;
import dev.lumenlang.lumen.pipeline.java.compiled.ClassBuilder;
import dev.lumenlang.lumen.pipeline.java.compiled.ScriptSourceMap;
import dev.lumenlang.lumen.pipeline.language.emit.CodeEmitter;
import dev.lumenlang.lumen.pipeline.language.emit.TransformerRegistry;
import dev.lumenlang.lumen.pipeline.language.exceptions.LumenScriptException;
import dev.lumenlang.lumen.pipeline.language.incremental.ScriptMatchCache;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.plugin.configuration.LumenConfiguration;
import dev.lumenlang.lumen.plugin.scripts.model.source.GeneratedSource;
import dev.lumenlang.lumen.plugin.scripts.model.source.ScriptSource;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parses {@code .luma} sources into emitted Java sources.
 */
public final class ScriptParser {

    private static final Map<String, ScriptMatchCache> MATCH_CACHES = new ConcurrentHashMap<>();

    private ScriptParser() {
    }

    public static @NotNull GeneratedSource parse(@NotNull String name, @NotNull String source) {
        CodegenContextImpl gen = new CodegenContextImpl(name);
        gen.setRawJavaEnabled(LumenConfiguration.LANGUAGE.EXPERIMENTAL.RAW_JAVA);
        CodeEmitter.setParallelParseThreads(LumenConfiguration.PERFORMANCE.PARALLEL_PARSE_THREADS);
        JavaBuilder output = new JavaBuilder();
        TypeEnvImpl env = new TypeEnvImpl();

        ScriptMatchCache cache = MATCH_CACHES.computeIfAbsent(name, k -> new ScriptMatchCache());
        CodeEmitter.generate(source, PatternRegistry.instance(), env, gen, output, cache);

        if (LumenConfiguration.LANGUAGE.EXPERIMENTAL.CODE_TRANSFORM) {
            TransformerRegistry.instance().transform(output, gen);
        }

        String javaSource = ClassBuilder.buildClass(gen.className(), gen, output);
        String fqcn = ClassBuilder.fqcn(gen.className());

        ScriptSourceMap.register(fqcn, javaSource);

        return new GeneratedSource(name, gen.className(), javaSource, source, fqcn);
    }

    /**
     * Parses many scripts, logging and skipping any that fail.
     */
    public static @NotNull List<GeneratedSource> parseAll(@NotNull List<ScriptSource> sources) {
        List<GeneratedSource> generated = new ArrayList<>();
        for (ScriptSource ss : sources) {
            try {
                generated.add(parse(ss.name(), ss.source()));
            } catch (LumenScriptException e) {
                if (e.diagnostic() != null || e.getMessage().contains("\n")) {
                    LumenLogger.severe("Script error in " + ss.name() + ":\n" + e.getMessage());
                } else {
                    LumenLogger.severe("Script error in " + ss.name() + ": " + e.getMessage());
                }
            } catch (Throwable t) {
                LumenLogger.severe("Failed to generate script: " + ss.name(), t);
            }
        }
        return generated;
    }

    public static void invalidateCache(@NotNull String name) {
        MATCH_CACHES.remove(name);
    }
}
