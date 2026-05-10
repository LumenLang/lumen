package dev.lumenlang.lumen.plugin.scripts;

import dev.lumenlang.lumen.pipeline.java.compiled.ClassBuilder;
import dev.lumenlang.lumen.pipeline.java.compiled.ScriptSourceMap;
import dev.lumenlang.lumen.pipeline.java.compiler.ScriptClassLoader;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.plugin.Lumen;
import dev.lumenlang.lumen.plugin.configuration.LumenConfiguration;
import dev.lumenlang.lumen.plugin.inject.bytecode.InjectableRegistry;
import dev.lumenlang.lumen.plugin.scripts.cache.CompiledClassCache;
import dev.lumenlang.lumen.plugin.scripts.model.compiled.CompileTimings;
import dev.lumenlang.lumen.plugin.scripts.model.compiled.PreparedScript;
import dev.lumenlang.lumen.plugin.scripts.model.source.GeneratedSource;
import dev.lumenlang.lumen.plugin.scripts.model.source.ScriptSource;
import dev.lumenlang.lumen.plugin.scripts.pipeline.ScriptCompilation;
import dev.lumenlang.lumen.plugin.scripts.pipeline.ScriptParser;
import dev.lumenlang.lumen.plugin.scripts.runtime.LoadedScript;
import dev.lumenlang.lumen.plugin.scripts.runtime.ScriptActivator;
import dev.lumenlang.lumen.plugin.scripts.runtime.ScriptLifecycle;
import dev.lumenlang.lumen.plugin.scripts.source.ScriptSourceLoader;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

/**
 * Loads and warms up Lumen scripts. Lifecycle queries (isLoaded, unload, ...)
 * live on {@link ScriptLifecycle}.
 */
public final class Scripts {

    private Scripts() {
    }

    /**
     * Loads or reloads a single script. Parse and compile run on the common
     * pool; the bind step runs on the main server thread.
     */
    public static @NotNull CompletableFuture<CompileTimings> load(@NotNull String name, @NotNull String source) {
        return CompletableFuture.supplyAsync(() -> {
            PreparedScript prepared = prepareOne(name, source);
            Map<String, ScriptClassLoader> loaders = LumenConfiguration.PERFORMANCE.ASYNC_DEFINE_CLASS ? Map.of(prepared.fqcn(), ScriptActivator.buildLoader(prepared.bytecodes())) : Map.of();
            return Map.entry(prepared, loaders);
        }).thenCompose(entry -> {
            PreparedScript prepared = entry.getKey();
            Map<String, ScriptClassLoader> loaders = entry.getValue();
            CompletableFuture<CompileTimings> future = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(Lumen.instance(), () -> {
                try {
                    if (ScriptLifecycle.tearDownForReload(name)) {
                        ScriptLifecycle.postUnloaded(name);
                    }
                    bind(prepared, loaders.get(prepared.fqcn()));
                    ScriptLifecycle.postLoaded(name);
                    future.complete(prepared.timings());
                } catch (Throwable t) {
                    ScriptLifecycle.unload(name);
                    future.completeExceptionally(t);
                }
            });
            return future;
        });
    }

    /**
     * Loads every script in the directory off-thread, then binds on the main thread.
     */
    public static @NotNull CompletableFuture<List<PreparedScript>> loadAll() {
        List<String> names = ScriptSourceLoader.list();
        if (names.isEmpty()) return CompletableFuture.completedFuture(List.of());

        return CompletableFuture.supplyAsync(() -> {
            List<PreparedScript> prepared = prepareAll(names);
            Map<String, ScriptClassLoader> loaders = LumenConfiguration.PERFORMANCE.ASYNC_DEFINE_CLASS ? buildLoaders(prepared) : Map.of();
            return Map.entry(prepared, loaders);
        }).thenCompose(entry -> {
            List<PreparedScript> prepared = entry.getKey();
            Map<String, ScriptClassLoader> loaders = entry.getValue();
            CompletableFuture<List<PreparedScript>> future = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(Lumen.instance(), () -> {
                try {
                    bindAll(names, prepared, loaders);
                    future.complete(prepared);
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
            return future;
        });
    }

    /**
     * Prepares and binds every script on the calling thread.
     */
    public static @NotNull List<PreparedScript> loadAllBlocking() {
        List<String> names = ScriptSourceLoader.list();
        if (names.isEmpty()) return List.of();

        List<PreparedScript> prepared = prepareAll(names);
        bindAll(names, prepared, Map.of());
        return prepared;
    }

    /**
     * Walks parser, code emitter, and Java compiler with a bundled example so
     * the first real script load has no cold-start penalty.
     */
    public static void warmup() {
        try (var in = Scripts.class.getClassLoader().getResourceAsStream("examples/economy.luma")) {
            if (in == null) {
                LumenLogger.warning("Full warmup script (examples/economy.luma) not found in jar, falling back to compiler warmup.");
                return;
            }
            String source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            prepareOne("__warmup__.luma", source);
            deleteWarmupDir();
        } catch (Exception e) {
            LumenLogger.warning("Full warmup failed! Error: " + e.getMessage() + ".");
        }
    }

    private static void bindAll(@NotNull List<String> names, @NotNull List<PreparedScript> prepared, @NotNull Map<String, ScriptClassLoader> loaders) {
        for (String name : names) {
            if (ScriptLifecycle.tearDownForReload(name)) {
                ScriptLifecycle.postUnloaded(name);
            }
        }
        List<String> loaded = new ArrayList<>();
        for (PreparedScript p : prepared) {
            try {
                bind(p, loaders.get(p.fqcn()));
                ScriptLifecycle.postLoaded(p.name());
                loaded.add(p.name());
            } catch (Throwable t) {
                LumenLogger.severe("Failed to load script: " + p.name(), t);
            }
        }
        ScriptLifecycle.postAllLoaded(loaded);
    }

    private static void bind(@NotNull PreparedScript p, @Nullable ScriptClassLoader preBuiltLoader) {
        LoadedScript loaded = ScriptActivator.activate(p.name(), p.fqcn(), p.bytecodes(), preBuiltLoader);
        ScriptLifecycle.register(p.name(), loaded);
    }

    private static @NotNull Map<String, ScriptClassLoader> buildLoaders(@NotNull List<PreparedScript> prepared) {
        Map<String, ScriptClassLoader> loaders = new HashMap<>();
        for (PreparedScript p : prepared) {
            loaders.put(p.fqcn(), ScriptActivator.buildLoader(p.bytecodes()));
        }
        return loaders;
    }

    private static @NotNull List<PreparedScript> prepareAll(@NotNull List<String> names) {
        List<PreparedScript> all = new ArrayList<>();
        List<ScriptSource> toParse = new ArrayList<>();

        int cacheHits = 0;
        for (String name : names) {
            try {
                String src = ScriptSourceLoader.load(name);
                if (LumenConfiguration.PERFORMANCE.CACHE_COMPILED_CLASSES) {
                    PreparedScript hit = tryCache(name, src);
                    if (hit != null) {
                        all.add(hit);
                        cacheHits++;
                        continue;
                    }
                }
                toParse.add(new ScriptSource(name, src));
            } catch (Throwable t) {
                LumenLogger.severe("Failed to load script source: " + name, t);
            }
        }

        if (LumenConfiguration.PERFORMANCE.CACHE_COMPILED_CLASSES) {
            LumenLogger.info("Cache: " + cacheHits + " hit(s), " + toParse.size() + " miss(es).");
        }

        if (!toParse.isEmpty()) {
            long parseStart = System.nanoTime();
            List<GeneratedSource> generated = ScriptParser.parseAll(toParse);
            long parseTotalNanos = System.nanoTime() - parseStart;

            if (!generated.isEmpty()) {
                double totalMs = parseTotalNanos / 1_000_000.0;
                LumenLogger.info("Parsed " + generated.size() + " scripts in " + String.format("%.6f", totalMs) + " ms | parse(avg)=" + String.format("%.6f", totalMs / generated.size()) + " ms");
                all.addAll(ScriptCompilation.compileBatch(generated, parseTotalNanos));
            }
        }

        return all;
    }

    private static @NotNull PreparedScript prepareOne(@NotNull String name, @NotNull String source) {
        if (!name.startsWith("__") && LumenConfiguration.PERFORMANCE.CACHE_COMPILED_CLASSES) {
            PreparedScript hit = tryCache(name, source);
            if (hit != null) {
                if (LumenConfiguration.DEBUG.LOG_COMPILATION) {
                    LumenLogger.info("[Compilation] Loaded " + name + " from cache");
                }
                return hit;
            }
        }

        long parseStart = System.nanoTime();
        GeneratedSource generated = ScriptParser.parse(name, source);
        long parseNanos = System.nanoTime() - parseStart;

        if (LumenConfiguration.DEBUG.LOG_COMPILATION) {
            LumenLogger.info("[Compilation] Parsed " + name + " in " + String.format("%.3f", parseNanos / 1_000_000.0) + " ms");
        }

        try {
            return ScriptCompilation.compileOne(generated, parseNanos);
        } catch (RuntimeException e) {
            InjectableRegistry.clear(generated.fqcn());
            throw e;
        }
    }

    private static @Nullable PreparedScript tryCache(@NotNull String name, @NotNull String source) {
        Map<String, byte[]> cached = CompiledClassCache.load(name, source);
        if (cached == null) return null;
        String fqcn = ClassBuilder.fqcn(name);
        String cachedJavaSource = CompiledClassCache.loadJavaSource(name);
        if (cachedJavaSource != null) {
            ScriptSourceMap.register(fqcn, cachedJavaSource);
        }
        return new PreparedScript(name, fqcn, cached, new CompileTimings(0L, 0L));
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
}
