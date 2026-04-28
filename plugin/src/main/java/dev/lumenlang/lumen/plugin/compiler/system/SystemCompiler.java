package dev.lumenlang.lumen.plugin.compiler.system;

import dev.lumenlang.lumen.pipeline.java.compiled.ClassBuilder;
import dev.lumenlang.lumen.pipeline.java.compiler.CompilerClasspath;
import dev.lumenlang.lumen.plugin.compiler.JavaCompilerBackend;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compiler backend that delegates to the system Java compiler (javac).
 */
public final class SystemCompiler implements JavaCompilerBackend {

    private static final ThreadLocal<StandardJavaFileManager> THREAD_FM = ThreadLocal.withInitial(() -> {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new RuntimeException("No system Java compiler available");
        return compiler.getStandardFileManager(null, null, null);
    });
    private static final Set<String> EXCLUDED_PACKAGES = Set.of(
            "/io/netty/",
            "/com/velocitypowered/",
            "/com/github/oshi/",
            "/org/apache/maven/",
            "/org/codehaus/plexus/",
            "/org/eclipse/sisu/",
            "/net/java/dev/jna/",
            "/org/jline/",
            "/org/fusesource/jansi/",
            "/com/lmax/disruptor/",
            "/org/apache/logging/",
            "/org/slf4j/",
            "/net/minecrell/",
            "/org/apache/httpcomponents/",
            "/com/mysql/",
            "/org/xerial/",
            "/it/unimi/dsi/",
            "/com/mojang/datafixerupper/",
            "/com/mojang/logging/",
            "/net/fabricmc/",
            "/io/leangen/",
            "/org/ow2/asm/",
            "/org/spongepowered/configurate",
            "/commons-codec/",
            "/com/google/errorprone/",
            "/com/google/j2objc/",
            "/com/google/protobuf/"
    );

    private volatile JavaCompiler cachedCompiler;

    /**
     * Returns whether the system Java compiler is available on this JVM.
     *
     * @return true if javac is accessible
     */
    public static boolean isAvailable() {
        return ToolProvider.getSystemJavaCompiler() != null;
    }

    private static @NotNull String extractFqcn(@Nullable JavaFileObject source) {
        if (source == null) return "";
        String uri = source.toUri().toString();
        return uri.replaceFirst("^string:///", "").replace('/', '.').replaceAll("\\.java$", "");
    }

    private static @NotNull String classpath() {
        StringBuilder sb = new StringBuilder();
        ClassLoader cl = ClassBuilder.class.getClassLoader();
        boolean reduce = CompilerClasspath.reduceClasspath();
        while (cl != null) {
            if (cl instanceof URLClassLoader ucl) {
                for (var url : ucl.getURLs()) {
                    String path = url.getPath();
                    if (reduce && isExcluded(path)) continue;
                    if (!sb.isEmpty()) sb.append(File.pathSeparator);
                    sb.append(new File(path));
                }
            }
            cl = cl.getParent();
        }
        for (String extra : CompilerClasspath.entries()) {
            if (!sb.isEmpty()) sb.append(File.pathSeparator);
            sb.append(extra);
        }
        return sb.toString();
    }

    private static boolean isExcluded(@NotNull String path) {
        for (String excluded : EXCLUDED_PACKAGES) {
            if (path.contains(excluded)) return true;
        }
        return false;
    }

    @Override
    public @NotNull Map<String, byte[]> compileAll(@NotNull List<SourceFile> files) {
        JavaCompiler compiler = compiler();
        InMemoryFileManager fm = new InMemoryFileManager(THREAD_FM.get());

        List<String> options = List.of("-g", "-classpath", classpath());

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        Boolean ok = compiler.getTask(null, fm, diagnostics, options, null, files).call();

        if (ok == null || !ok) {
            List<CompilationFailedException.CompileError> errors = new ArrayList<>();
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                if (d.getKind() == Diagnostic.Kind.ERROR) {
                    errors.add(new CompilationFailedException.CompileError(extractFqcn(d.getSource()), d.getLineNumber(), d.getMessage(null)));
                }
            }
            throw new CompilationFailedException(errors);
        }

        return fm.classes;
    }

    private @NotNull JavaCompiler compiler() {
        JavaCompiler c = cachedCompiler;
        if (c == null) {
            synchronized (this) {
                c = cachedCompiler;
                if (c == null) {
                    c = ToolProvider.getSystemJavaCompiler();
                    if (c == null) throw new RuntimeException("No system Java compiler available");
                    cachedCompiler = c;
                }
            }
        }
        return c;
    }
}
