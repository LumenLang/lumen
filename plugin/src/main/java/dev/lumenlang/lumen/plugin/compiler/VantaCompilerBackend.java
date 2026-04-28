package dev.lumenlang.lumen.plugin.compiler;

import dev.lumenlang.lumen.pipeline.java.compiled.ClassBuilder;
import dev.lumenlang.lumen.pipeline.java.compiler.CompilerClasspath;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import dev.lumenlang.lumen.plugin.compiler.system.SourceFile;
import dev.lumenlang.lumen.plugin.compiler.system.SystemCompiler;
import dev.lumenlang.lumen.plugin.configuration.LumenConfiguration;
import net.vansencool.vanta.ParallelMode;
import net.vansencool.vanta.VantaCompiler;
import net.vansencool.vanta.classpath.ClasspathManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compiler backend that uses Vanta for bytecode generation.
 *
 * <p>Vanta compiles Java source directly to JVM bytecode without invoking the system
 * Java compiler, offering significantly faster compilation in hot and cold JVMs.
 * Vanta is currently in beta.
 */
public final class VantaCompilerBackend implements JavaCompilerBackend {

    private static final Set<String> EXCLUDED_PACKAGES = Set.of(
            "/io/netty/", "/com/velocitypowered/", "/com/github/oshi/", "/org/apache/maven/",
            "/org/codehaus/plexus/", "/org/eclipse/sisu/", "/net/java/dev/jna/", "/org/jline/",
            "/org/fusesource/jansi/", "/com/lmax/disruptor/", "/org/apache/logging/", "/org/slf4j/",
            "/net/minecrell/", "/org/apache/httpcomponents/", "/com/mysql/", "/org/xerial/",
            "/it/unimi/dsi/", "/com/mojang/datafixerupper/", "/com/mojang/logging/", "/net/fabricmc/",
            "/io/leangen/", "/org/ow2/asm/", "/org/spongepowered/configurate", "/commons-codec/",
            "/com/google/errorprone/", "/com/google/j2objc/", "/com/google/protobuf/"
    );

    private static final ClasspathManager CLASSPATH = buildBaseClasspath(ClassBuilder.class.getClassLoader());
    private static final VantaCompiler COMPILER = new VantaCompiler(CLASSPATH);
    private static final Set<String> REGISTERED_EXTRAS = ConcurrentHashMap.newKeySet();

    private final SystemCompiler javacFallback = SystemCompiler.isAvailable() ? new SystemCompiler() : null;

    private static @NotNull ClasspathManager buildBaseClasspath(@NotNull ClassLoader cl) {
        ClasspathManager cp = new ClasspathManager();
        boolean reduce = CompilerClasspath.reduceClasspath();
        for (ClassLoader c = cl; c != null; c = c.getParent()) {
            if (c instanceof URLClassLoader ucl) {
                for (var url : ucl.getURLs()) {
                    if (reduce && isExcluded(url.getPath())) continue;
                    cp.addEntry(Paths.get(new File(url.getPath()).getAbsolutePath()));
                }
            }
        }
        return cp;
    }

    private static void syncExtras() {
        for (String extra : CompilerClasspath.entries()) {
            if (REGISTERED_EXTRAS.add(extra)) CLASSPATH.addEntry(Paths.get(extra));
        }
    }

    private static boolean isExcluded(@NotNull String path) {
        for (String excluded : EXCLUDED_PACKAGES) {
            if (path.contains(excluded)) return true;
        }
        return false;
    }

    @Override
    public @NotNull Map<String, byte[]> compileAll(@NotNull List<SourceFile> files) {
        syncExtras();

        Map<String, byte[]> vantaResult = new HashMap<>();
        Map<String, String> vantaSources = new HashMap<>();
        for (SourceFile file : files) {
            try {
                vantaSources.put(file.toUri().getPath(), file.getCharContent(true).toString());
            } catch (Exception e) {
                throw new RuntimeException("Failed to read source file: " + file.toUri().getPath(), e);
            }
        }

        try {
            Map<String, byte[]> compiled = COMPILER.compileAllParallel(vantaSources, ParallelMode.files(LumenConfiguration.PERFORMANCE.COMPILE_THREADS));
            for (var entry : compiled.entrySet()) {
                vantaResult.put(entry.getKey().replace('/', '.'), entry.getValue());
            }
        } catch (Throwable t) {
            LumenLogger.warning("[Vanta] Unexpected failure: " + t.getMessage() + ". Falling back to system compiler.");
            vantaResult.clear();
            if (javacFallback == null) {
                LumenLogger.severe("[Vanta] System compiler not available, cannot compile scripts!");
                return Map.of();
            }
            javacFallback.compileAll(files).forEach((name, bytes) -> {
                if (!vantaResult.containsKey(name)) {
                    vantaResult.put(name, bytes);
                }
            });
        }

        return vantaResult;
    }
}
