package dev.lumenlang.lumen.plugin.scripts;

import dev.lumenlang.lumen.pipeline.annotations.LumenLoad;
import dev.lumenlang.lumen.pipeline.annotations.LumenPreload;
import dev.lumenlang.lumen.pipeline.codegen.CodegenContext;
import dev.lumenlang.lumen.pipeline.codegen.TypeEnv;
import dev.lumenlang.lumen.pipeline.java.JavaBuilder;
import dev.lumenlang.lumen.pipeline.java.compiled.ScriptRuntime;
import dev.lumenlang.lumen.pipeline.java.compiled.ScriptSourceMap;
import dev.lumenlang.lumen.pipeline.java.compiler.ScriptClassLoader;
import dev.lumenlang.lumen.pipeline.java.compiler.system.CompilationFailedException;
import dev.lumenlang.lumen.pipeline.java.compiler.system.InMemoryFileManager;
import dev.lumenlang.lumen.pipeline.java.compiler.system.SourceFile;
import dev.lumenlang.lumen.pipeline.java.compiler.system.SystemCompiler;
import dev.lumenlang.lumen.pipeline.java.formatter.MiniJavaFormatter;
import dev.lumenlang.lumen.pipeline.language.emit.CodeEmitter;
import dev.lumenlang.lumen.pipeline.language.exceptions.LumenScriptException;
import dev.lumenlang.lumen.pipeline.language.pattern.PatternRegistry;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.pipeline.persist.GlobalVars;
import dev.lumenlang.lumen.plugin.Lumen;
import dev.lumenlang.lumen.plugin.binder.ScriptBinder;
import dev.lumenlang.lumen.plugin.commands.CommandRegistry;
import dev.lumenlang.lumen.plugin.configuration.LumenConfiguration;
import dev.lumenlang.lumen.plugin.scheduler.ScriptScheduler;
import dev.lumenlang.lumen.plugin.util.InventoryRegistry;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages the lifecycle of Lumen scripts: parsing, compiling, loading, and
 * unloading.
 *
 * <p>
 * Each script goes through three phases:
 * <ol>
 * <li><b>Parse</b>: the {@code .luma} source is converted into Java source code
 * via
 * {@link CodeEmitter}.</li>
 * <li><b>Compile</b>: the generated Java source is compiled to bytecodes via
 * the
 * system compiler ({@code javac}).</li>
 * <li><b>Load</b>: the bytecodes are loaded into a {@link ScriptClassLoader},
 * the
 * class is instantiated, and event/command bindings are registered.</li>
 * </ol>
 *
 * <p>
 * Parsing and compilation run on a background thread pool. Only the binding
 * phase
 * (event registration, command registration, lifecycle hooks) runs on the main
 * server thread.
 *
 * <p>
 * The optional {@link CompiledClassCache} allows skipping the parse and compile
 * phases
 * entirely when a script has not changed since the last compilation.
 */
@SuppressWarnings("resource")
public final class ScriptManager {

    private static final Map<String, LoadedScript> scripts = new ConcurrentHashMap<>();

    private static final ExecutorService COMPILE_POOL = Executors.newFixedThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() / 2),
            r -> {
                Thread t = new Thread(r, "Lumen-Compile");
                t.setDaemon(true);
                return t;
            });

    /**
     * Asynchronously loads (or reloads) a single script by name.
     *
     * <p>
     * Parsing and compilation run on a background thread pool. Once compiled,
     * the binding phase (unloading the old script, loading bytecodes, registering
     * event/commands, and invoking lifecycle hooks) runs on the main server thread.
     *
     * @param name   the script file name (e.g. {@code "hello.luma"})
     * @param source the raw {@code .luma} source text
     * @return a future that completes with {@link CompileTimings} on the main
     * thread
     */
    public static @NotNull CompletableFuture<CompileTimings> load(@NotNull String name, @NotNull String source) {
        return CompletableFuture.supplyAsync(() -> prepareScript(name, source), COMPILE_POOL)
                .thenCompose(prepared -> {
                    CompletableFuture<CompileTimings> future = new CompletableFuture<>();
                    Bukkit.getScheduler().runTask(Lumen.instance(), () -> {
                        try {
                            reload(name);
                            loadBytecodes(name, prepared.fqcn(), prepared.bytecodes());
                            future.complete(prepared.timings());
                        } catch (Throwable t) {
                            unload(name);
                            future.completeExceptionally(t);
                        }
                    });
                    return future;
                });
    }

    /**
     * Unloads a script, unbinding all event listeners and unregistering all
     * commands
     * it defined. All pending scheduled tasks are cancelled unconditionally.
     *
     * @param name the script file name
     */
    public static void unload(@NotNull String name) {
        LoadedScript s = scripts.remove(name);
        if (s == null)
            return;

        String normalized = ScriptRuntime.normalize(new CodegenContext(name).className());
        String fqcn = "dev.lumenlang.lumen.java.compiled." + normalized;
        ScriptSourceMap.unregisterByClassName(normalized);
        ScriptBinder.unbindAll(s.instance());
        CommandRegistry.unregisterScript(name);
        ScriptScheduler.handleUnload(fqcn);
    }

    /**
     * Tears down a script in preparation for an immediate reload. Event listeners
     * and commands are unregistered, but scheduled task handling follows the reload
     * configuration rather than cancelling unconditionally.
     *
     * @param name the script file name
     */
    private static void reload(@NotNull String name) {
        LoadedScript s = scripts.remove(name);
        if (s == null)
            return;

        String normalized = ScriptRuntime.normalize(new CodegenContext(name).className());
        String fqcn = "dev.lumenlang.lumen.java.compiled." + normalized;
        ScriptSourceMap.unregisterByClassName(normalized);
        ScriptBinder.unbindAll(s.instance());
        CommandRegistry.unregisterScript(name);
        ScriptScheduler.handleReload(fqcn);
        GlobalVars.deleteByPrefix(normalized + ".");
        InventoryRegistry.clearInstance(s.instance());
    }

    /**
     * Returns whether a script with the given name is currently loaded.
     *
     * @param name the script file name
     * @return {@code true} if the script is loaded
     */
    public static boolean isLoaded(@NotNull String name) {
        return scripts.containsKey(name);
    }

    /**
     * Asynchronously loads all scripts from the scripts directory.
     *
     * <p>
     * Source reading, cache checking, parsing, and compilation all run on a
     * background thread pool. Once all scripts are prepared, the binding phase
     * (unloading old scripts and registering new ones) runs on the main server
     * thread.
     *
     * @return a future that completes when all scripts are loaded
     */
    public static @NotNull CompletableFuture<List<PreparedScript>> loadAll() {
        List<String> names = ScriptSourceLoader.list();
        if (names.isEmpty())
            return CompletableFuture.completedFuture(List.of());

        return CompletableFuture.supplyAsync(() -> {
            List<PreparedScript> allPrepared = new ArrayList<>();
            List<ScriptSource> toParse = new ArrayList<>();

            boolean cacheEnabled = LumenConfiguration.PERFORMANCE.CACHE_COMPILED_CLASSES;
            int cacheHits = 0;

            for (String name : names) {
                try {
                    String src = ScriptSourceLoader.load(name);

                    if (cacheEnabled) {
                        Map<String, byte[]> cached = CompiledClassCache.load(name, src);
                        if (cached != null) {
                            String fqcn = "dev.lumenlang.lumen.java.compiled." +
                                    ScriptRuntime.normalize(new CodegenContext(name).className());
                            String cachedJavaSource = CompiledClassCache.loadJavaSource(name);
                            if (cachedJavaSource != null) {
                                ScriptSourceMap.register(fqcn, cachedJavaSource);
                            }
                            allPrepared.add(new PreparedScript(name, fqcn, cached,
                                    new CompileTimings(0L, 0L)));
                            cacheHits++;
                            continue;
                        }
                    }

                    toParse.add(new ScriptSource(name, src));
                } catch (Throwable t) {
                    LumenLogger.severe("Failed to load script source: " + name, t);
                }
            }

            if (cacheEnabled) {
                LumenLogger.info("Cache: " + cacheHits + " hit(s), " + toParse.size() + " miss(es).");
            }

            if (!toParse.isEmpty()) {
                long parseStart = System.nanoTime();
                List<GeneratedSource> generated = parseAll(toParse);

                double totalMs = (System.nanoTime() - parseStart) / 1_000_000.0;
                if (!generated.isEmpty()) {
                    double avgMs = totalMs / generated.size();
                    LumenLogger.info(
                            "Parsed " + generated.size() + " scripts in " +
                                    String.format("%.6f", totalMs) +
                                    " ms | parse(avg)=" + String.format("%.6f", avgMs) + " ms");

                    allPrepared.addAll(batchCompile(generated,
                            (long) (totalMs * 1_000_000)));
                }
            }

            return allPrepared;
        }, COMPILE_POOL).thenCompose(prepared -> {
            CompletableFuture<List<PreparedScript>> future = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(Lumen.instance(), () -> {
                try {
                    GlobalVars.clear();
                    InventoryRegistry.clear();
                    names.forEach(ScriptManager::reload);
                    for (PreparedScript p : prepared) {
                        try {
                            loadBytecodes(p.name(), p.fqcn(), p.bytecodes());
                        } catch (Throwable t) {
                            LumenLogger.severe("Failed to load script: " + p.name(), t);
                        }
                    }
                    future.complete(prepared);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
            return future;
        });
    }

    /**
     * Synchronously loads all scripts from the scripts directory.
     *
     * <p>
     * All phases (parsing, compilation, and binding) run on the calling thread.
     * This is intended for server startup where scripts must be fully loaded before
     * the server continues initialization.
     */
    public static void loadAllSync() {
        List<String> names = ScriptSourceLoader.list();
        if (names.isEmpty())
            return;

        List<PreparedScript> allPrepared = new ArrayList<>();
        List<ScriptSource> toParse = new ArrayList<>();

        boolean cacheEnabled = LumenConfiguration.PERFORMANCE.CACHE_COMPILED_CLASSES;
        int cacheHits = 0;

        for (String name : names) {
            try {
                String src = ScriptSourceLoader.load(name);

                if (cacheEnabled) {
                    Map<String, byte[]> cached = CompiledClassCache.load(name, src);
                    if (cached != null) {
                        String fqcn = "dev.lumenlang.lumen.java.compiled." + ScriptRuntime.normalize(new CodegenContext(name).className());
                        String cachedJavaSource = CompiledClassCache.loadJavaSource(name);
                        if (cachedJavaSource != null) {
                            ScriptSourceMap.register(fqcn, cachedJavaSource);
                        }
                        allPrepared.add(new PreparedScript(name, fqcn, cached, new CompileTimings(0L, 0L)));
                        cacheHits++;
                        continue;
                    }
                }

                toParse.add(new ScriptSource(name, src));
            } catch (Throwable t) {
                LumenLogger.severe("Failed to load script source: " + name, t);
            }
        }

        if (cacheEnabled) {
            LumenLogger.info("Cache: " + cacheHits + " hit(s), " + toParse.size() + " miss(es).");
        }

        if (!toParse.isEmpty()) {
            long parseStart = System.nanoTime();
            List<GeneratedSource> generated = parseAll(toParse);

            double totalMs = (System.nanoTime() - parseStart) / 1_000_000.0;
            if (!generated.isEmpty()) {
                double avgMs = totalMs / generated.size();
                LumenLogger.info(
                        "Parsed " + generated.size() + " scripts in " +
                                String.format("%.6f", totalMs) +
                                " ms | parse(avg)=" + String.format("%.6f", avgMs) + " ms");

                allPrepared.addAll(batchCompile(generated, (long) (totalMs * 1_000_000)));
            }
        }

        GlobalVars.clear();
        InventoryRegistry.clear();
        names.forEach(ScriptManager::reload);
        for (PreparedScript p : allPrepared) {
            try {
                loadBytecodes(p.name(), p.fqcn(), p.bytecodes());
            } catch (Throwable t) {
                LumenLogger.severe("Failed to load script: " + p.name(), t);
            }
        }
    }

    /**
     * Shuts down the script compilation thread pool.
     */
    public static void shutdownPool() {
        COMPILE_POOL.shutdownNow();
    }

    /**
     * Runs a full pipeline warmup by reading, parsing, and compiling a bundled
     * example script. The compiled output is discarded. This exercises the
     * parser, code emitter, and Java compiler so that the first real script
     * load has no cold-start penalty.
     *
     * <p>If the bundled script cannot be found (e.g. it was stripped from the jar),
     * a warning is logged and the method falls back to a compiler-only warmup.
     */
    public static void warmup() {
        try (var in = ScriptManager.class.getClassLoader()
                .getResourceAsStream("examples/economy.luma")) {
            if (in == null) {
                LumenLogger.warning("Full warmup script (examples/economy.luma) not found in jar, falling back to compiler warmup.");
                return;
            }
            String source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            prepareScript("__warmup__.luma", source);
            deleteWarmupDir();
        } catch (Exception e) {
            LumenLogger.warning("Full warmup failed! Error: " + e.getMessage() + ".");
        }
    }

    private static void deleteWarmupDir() {
        Path warmupDir = CompiledClassCache.compiledRoot().resolve("__warmup__.luma");
        if (!Files.isDirectory(warmupDir)) return;
        try (var stream = Files.walk(warmupDir)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException e) {
            LumenLogger.warning("Could not fully clean up warmup directory: " + e.getMessage());
        }
    }

    private static @NotNull PreparedScript prepareScript(@NotNull String name, @NotNull String source) {
        if (LumenConfiguration.PERFORMANCE.CACHE_COMPILED_CLASSES) {
            Map<String, byte[]> cached = CompiledClassCache.load(name, source);
            if (cached != null) {
                String fqcn = "dev.lumenlang.lumen.java.compiled." +
                        ScriptRuntime.normalize(new CodegenContext(name).className());

                if (LumenConfiguration.DEBUG.LOG_COMPILATION) {
                    LumenLogger.info("[Compilation] Loaded " + name + " from cache");
                }

                return new PreparedScript(name, fqcn, cached, new CompileTimings(0L, 0L));
            }
        }

        long parseStart = System.nanoTime();
        GeneratedSource generated = parse(name, source);
        long parseTime = System.nanoTime() - parseStart;

        if (LumenConfiguration.DEBUG.LOG_COMPILATION) {
            LumenLogger.info("[Compilation] Parsed " + name + " in "
                    + String.format("%.3f", parseTime / 1_000_000.0) + " ms");
        }

        long compileStart = System.nanoTime();
        dumpIfEnabled(generated);
        Map<String, byte[]> bytecodes = compile(generated);
        cacheIfEnabled(name, source, generated.javaSource(), bytecodes);
        long compileTime = System.nanoTime() - compileStart;

        if (LumenConfiguration.DEBUG.LOG_COMPILATION) {
            LumenLogger.info("[Compilation] Compiled " + name + " in "
                    + String.format("%.3f", compileTime / 1_000_000.0) + " ms");
        }

        return new PreparedScript(name, generated.fqcn(), bytecodes,
                new CompileTimings(parseTime, compileTime));
    }

    private static @NotNull GeneratedSource parse(@NotNull String name, @NotNull String source) {
        CodegenContext gen = new CodegenContext(name);
        gen.setRawJavaEnabled(LumenConfiguration.LANGUAGE.EXPERIMENTAL.RAW_JAVA);
        JavaBuilder output = new JavaBuilder();
        TypeEnv env = new TypeEnv();

        CodeEmitter.generate(source, PatternRegistry.instance(), env, gen, output);

        String javaSource = ScriptRuntime.buildClass(gen.className(), gen, output);
        String fqcn = "dev.lumenlang.lumen.java.compiled." + ScriptRuntime.normalize(gen.className());

        ScriptSourceMap.register(fqcn, javaSource);

        return new GeneratedSource(name, gen.className(), javaSource, source, fqcn);
    }

    private static @NotNull List<GeneratedSource> parseAll(@NotNull List<ScriptSource> sources) {
        List<GeneratedSource> generated = new ArrayList<>();
        for (ScriptSource ss : sources) {
            try {
                generated.add(parse(ss.name(), ss.source()));
            } catch (LumenScriptException e) {
                LumenLogger.severe("Script error in " + ss.name() + ": " + e.getMessage());
            } catch (Throwable t) {
                LumenLogger.severe("Failed to generate script: " + ss.name(), t);
            }
        }
        return generated;
    }

    private static @NotNull Map<String, byte[]> compile(@NotNull GeneratedSource source) {
        try {
            return SystemCompiler.compileAll(
                    ScriptRuntime.class.getClassLoader(),
                    List.of(new SourceFile(source.fqcn(), source.javaSource()))).classes;
        } catch (CompilationFailedException e) {
            logCompileErrors(e.errors(), source.scriptName());
            throw new RuntimeException(
                    "Compilation failed for script: " + source.scriptName() + " (see error log above for details)", e);
        } catch (Exception e) {
            LumenLogger.severe("[Script " + source.scriptName() + "] Unexpected compiler error: " + e.getMessage());
            throw new RuntimeException("Compilation failed for script: " + source.scriptName(), e);
        }
    }

    /**
     * Compiles each script in {@code generated} individually, collecting as many
     * results as possible. Scripts that fail are logged and skipped.
     *
     * @param generated the scripts to compile
     * @return the successfully compiled scripts
     */
    private static @NotNull List<PreparedScript> individualFallback(@NotNull List<GeneratedSource> generated) {
        List<PreparedScript> result = new ArrayList<>();
        for (GeneratedSource gs : generated) {
            try {
                Map<String, byte[]> bytecodes = compile(gs);
                cacheIfEnabled(gs.scriptName(), gs.originalSource(), gs.javaSource(), bytecodes);
                result.add(new PreparedScript(gs.scriptName(), gs.fqcn(), bytecodes,
                        new CompileTimings(0L, 0L)));
            } catch (RuntimeException ignored) {
            }
        }
        return result;
    }

    private static void logCompileErrors(@NotNull List<CompilationFailedException.CompileError> errors,
                                         @NotNull String scriptName) {
        for (CompilationFailedException.CompileError err : errors) {
            ScriptSourceMap.ScriptLineMapping mapping = err.javaLine() > 0
                    ? ScriptSourceMap.findScriptLine(err.fqcn(), (int) err.javaLine())
                    : null;
            if (mapping != null) {
                LumenLogger.severe("[Script " + scriptName + "] Line " + mapping.scriptLine()
                        + ": " + mapping.scriptSource() + " -- " + err.message());
            } else {
                LumenLogger.severe("[Script " + scriptName + "] Compilation error: " + err.message());
            }
        }
        LumenLogger.severe("[Script " + scriptName + "] Got " + errors.size() + " compilation error(s).");
    }

    private static void logCompileErrors(@NotNull List<CompilationFailedException.CompileError> errors,
                                         @NotNull List<GeneratedSource> generated) {
        for (CompilationFailedException.CompileError err : errors) {
            String scriptName = generated.stream()
                    .filter(s -> s.fqcn().equals(err.fqcn()))
                    .map(GeneratedSource::scriptName)
                    .findFirst().orElse(err.fqcn());
            ScriptSourceMap.ScriptLineMapping mapping = err.javaLine() > 0
                    ? ScriptSourceMap.findScriptLine(err.fqcn(), (int) err.javaLine())
                    : null;
            if (mapping != null) {
                LumenLogger.severe("[Script " + scriptName + "] Line " + mapping.scriptLine()
                        + ": " + mapping.scriptSource() + " -- " + err.message());
            } else {
                LumenLogger.severe("[Script " + scriptName + "] Compilation error: " + err.message());
            }
        }
        LumenLogger.severe("Got " + errors.size() + " compilation error(s) across batch.");
    }

    private static @NotNull List<PreparedScript> batchCompile(@NotNull List<GeneratedSource> generated,
                                                              long parseTotalNanos) {
        List<SourceFile> files = new ArrayList<>();
        for (GeneratedSource s : generated) {
            dumpIfEnabled(s);
            files.add(new SourceFile(s.fqcn(), s.javaSource()));
        }

        long compileStart = System.nanoTime();
        InMemoryFileManager fm;
        try {
            fm = SystemCompiler.compileAll(ScriptRuntime.class.getClassLoader(), files);
        } catch (CompilationFailedException e) {
            logCompileErrors(e.errors(), generated);
            LumenLogger.severe("Batch compilation failed due to script errors, falling back to individual compilation...");
            return individualFallback(generated);
        } catch (Exception e) {
            LumenLogger.severe("Batch compilation failed (" + e.getMessage() + "), falling back to individual compilation...");
            LumenLogger.severe("-------------------------------");
            return individualFallback(generated);
        }

        List<PreparedScript> result = new ArrayList<>();
        long compileTotal = System.nanoTime() - compileStart;
        long parsePerScript = generated.isEmpty() ? 0 : parseTotalNanos / generated.size();
        long compilePerScript = generated.isEmpty() ? 0 : compileTotal / generated.size();
        for (GeneratedSource s : generated) {
            String normalized = ScriptRuntime.normalize(s.className());
            Map<String, byte[]> bytecodes = extractBytecodes(fm.classes, normalized);
            if (LumenConfiguration.DEBUG.LOG_COMPILATION) {
                LumenLogger.info(
                        "[Compilation] Extracted " + bytecodes.size() + " bytecode classes for " + s.scriptName());
            }
            cacheIfEnabled(s.scriptName(), s.originalSource(), s.javaSource(), bytecodes);
            result.add(new PreparedScript(s.scriptName(), s.fqcn(), bytecodes,
                    new CompileTimings(parsePerScript, compilePerScript)));
        }

        double compileAvgMs = compileTotal / (double) generated.size() / 1_000_000.0;

        if (LumenConfiguration.DEBUG.LOG_COMPILATION) {
            for (GeneratedSource s : generated) {
                LumenLogger.info("[Compilation] Compiled " + s.scriptName()
                        + " (class: " + s.className() + ")");
            }
        }

        LumenLogger.info(
                "Compiled " + generated.size() + " scripts in " +
                        String.format("%.6f", compileTotal / 1_000_000.0) +
                        " ms | compile(avg)=" + String.format("%.6f", compileAvgMs) + " ms");

        return result;
    }

    private static void loadBytecodes(@NotNull String scriptName,
                                      @NotNull String fqcn,
                                      @NotNull Map<String, byte[]> bytecodes) {
        ScriptClassLoader loader = new ScriptClassLoader(ScriptRuntime.class.getClassLoader());
        for (var entry : bytecodes.entrySet()) {
            loader.define(entry.getKey(), entry.getValue());
        }
        try {
            Class<?> main = loader.loadClass(fqcn);
            Object inst = main.getDeclaredConstructor().newInstance();
            ScriptBinder.invokeMethodWithAnnotation(inst, main, LumenPreload.class);
            ScriptBinder.bindAll(inst, main);
            ScriptBinder.invokeMethodWithAnnotation(inst, main, LumenLoad.class);
            scripts.put(scriptName, new LoadedScript(main, inst));
        } catch (Throwable t) {
            throw new RuntimeException("Failed to load compiled script: " + scriptName, t);
        }
    }

    private static void cacheIfEnabled(@NotNull String scriptName,
                                       @NotNull String originalSource,
                                       @NotNull String javaSource,
                                       @NotNull Map<String, byte[]> bytecodes) {
        if (LumenConfiguration.PERFORMANCE.CACHE_COMPILED_CLASSES) {
            CompiledClassCache.save(scriptName, originalSource, bytecodes);
            CompiledClassCache.saveJavaSource(scriptName, javaSource);
        }
    }

    private static void dumpIfEnabled(@NotNull GeneratedSource source) {
        if (!LumenConfiguration.DEBUG.DUMP_GENERATED_JAVA)
            return;

        String safeName = source.scriptName().replace('/', '_').replace('\\', '_');
        Path dumpDir = CompiledClassCache.compiledRoot().resolve(safeName).resolve("dump");
        try {
            Files.createDirectories(dumpDir);

            String baseName = ScriptRuntime.normalize(source.className());
            String raw = source.javaSource();

            Files.writeString(dumpDir.resolve(baseName + "-raw.java"), raw);
            Files.writeString(dumpDir.resolve(baseName + "-formatted.java"),
                    MiniJavaFormatter.format(raw));
            Files.writeString(dumpDir.resolve(baseName + "-readable.java"),
                    MiniJavaFormatter.formatReadable(raw));
        } catch (IOException e) {
            LumenLogger.severe("Failed to dump generated Java for " + source.scriptName() + ": " + e.getMessage());
        }
    }

    private static @NotNull Map<String, byte[]> extractBytecodes(@NotNull Map<String, byte[]> allClasses,
                                                                 @NotNull String normalizedName) {
        String prefix = "dev.lumenlang.lumen.java.compiled." + normalizedName;
        Map<String, byte[]> result = new HashMap<>();
        for (var entry : allClasses.entrySet()) {
            String name = entry.getKey();
            if (name.equals(prefix) || name.startsWith(prefix + "$")) {
                result.put(name, entry.getValue());
            }
        }

        if (result.isEmpty() && !allClasses.isEmpty()) {
            LumenLogger.severe("[CompiledClassCache] Failed to extract bytecodes! Looking for prefix: " + prefix);
            LumenLogger.severe("[CompiledClassCache] Available classes in fm.classes: " + allClasses.keySet());
        }

        return result;
    }

    /**
     * Holds parse and compile timings for a single script load.
     *
     * @param parseNanos   nanoseconds spent parsing (0 if loaded from cache)
     * @param compileNanos nanoseconds spent compiling/loading
     */
    public record CompileTimings(long parseNanos, long compileNanos) {
    }

    /**
     * A loaded script: its class and the live instance.
     */
    public record LoadedScript(Class<?> clazz, Object instance) {
    }

    /**
     * Parsed Java source ready for compilation.
     */
    public record GeneratedSource(
            String scriptName,
            String className,
            String javaSource,
            String originalSource,
            String fqcn) {
    }

    /**
     * Raw script source before parsing.
     */
    public record ScriptSource(@NotNull String name, @NotNull String source) {
    }

    /**
     * A script that has been parsed and compiled, ready to be loaded on the main
     * thread.
     *
     * @param name      the script file name
     * @param fqcn      the fully qualified class name
     * @param bytecodes the compiled bytecodes
     * @param timings   parse and compile timings
     */
    public record PreparedScript(
            @NotNull String name,
            @NotNull String fqcn,
            @NotNull Map<String, byte[]> bytecodes,
            @NotNull CompileTimings timings) {
    }
}
