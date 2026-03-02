package net.vansencool.lumen.pipeline.java.compiler.system;

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
import java.util.Set;

/**
 * Utility class for compiling Java source files in memory using the system Java compiler.
 */
public final class SystemCompiler {

    private static volatile JavaCompiler cachedCompiler;
    private static volatile StandardJavaFileManager cachedFileManager;
    private static volatile boolean reduceClasspath;

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

    /**
     * Enables or disables the reduced classpath mode.
     * When enabled, jars belonging to packages irrelevant to script compilation
     * (networking, logging, databases, build tooling, etc.) are excluded.
     *
     * @param enabled whether to enable reduced classpath
     */
    public static void setReduceClasspath(boolean enabled) {
        reduceClasspath = enabled;
    }

    /**
     * Pre-initializes the Java compiler and file manager on the current thread.
     * Useful for warming up the compiler ahead of the first real compilation.
     */
    public static void warmup() {
        compiler();
        fileManager();
    }

    /**
     * Compiles the given Java source files in memory and returns a file manager containing the compiled classes.
     *
     * @param parent the parent class loader to use for resolving dependencies during compilation
     * @param files  the Java source files to compile
     * @return an InMemoryFileManager containing the compiled classes
     * @throws RuntimeException           if no system Java compiler is available
     * @throws CompilationFailedException if compilation fails with errors
     */
    public static @NotNull InMemoryFileManager compileAll(@NotNull ClassLoader parent, @NotNull List<SourceFile> files) {
        JavaCompiler compiler = compiler();
        InMemoryFileManager fm = new InMemoryFileManager(fileManager());

        List<String> options = List.of(
                "-g",
                "-classpath", buildClasspath(parent)
        );

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        Boolean ok = compiler.getTask(null, fm, diagnostics, options, null, files).call();

        if (ok == null || !ok) {
            List<CompilationFailedException.CompileError> errors = new ArrayList<>();
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                if (d.getKind() == Diagnostic.Kind.ERROR) {
                    String fqcn = extractFqcn(d.getSource());
                    errors.add(new CompilationFailedException.CompileError(fqcn, d.getLineNumber(), d.getMessage(null)));
                }
            }
            throw new CompilationFailedException(errors);
        }

        return fm;
    }

    private static @NotNull JavaCompiler compiler() {
        JavaCompiler c = cachedCompiler;
        if (c == null) {
            synchronized (SystemCompiler.class) {
                c = cachedCompiler;
                if (c == null) {
                    c = ToolProvider.getSystemJavaCompiler();
                    if (c == null) {
                        throw new RuntimeException("No system Java compiler available");
                    }
                    cachedCompiler = c;
                }
            }
        }
        return c;
    }

    private static @NotNull StandardJavaFileManager fileManager() {
        StandardJavaFileManager fm = cachedFileManager;
        if (fm == null) {
            synchronized (SystemCompiler.class) {
                fm = cachedFileManager;
                if (fm == null) {
                    fm = compiler().getStandardFileManager(null, null, null);
                    cachedFileManager = fm;
                }
            }
        }
        return fm;
    }

    private static @NotNull String extractFqcn(@Nullable JavaFileObject source) {
        if (source == null) return "";
        String uri = source.toUri().toString();
        return uri.replaceFirst("^string:///", "").replace('/', '.').replaceAll("\\.java$", "");
    }

    private static @NotNull String buildClasspath(@NotNull ClassLoader cl) {
        StringBuilder sb = new StringBuilder();

        while (cl != null) {
            if (cl instanceof URLClassLoader ucl) {
                for (var url : ucl.getURLs()) {
                    String path = url.getPath();
                    if (reduceClasspath && isExcluded(path)) continue;
                    if (!sb.isEmpty()) sb.append(File.pathSeparator);
                    sb.append(new File(path));
                }
            }
            cl = cl.getParent();
        }

        return sb.toString();
    }

    private static boolean isExcluded(@NotNull String path) {
        for (String excluded : EXCLUDED_PACKAGES) {
            if (path.contains(excluded)) return true;
        }
        return false;
    }
}
