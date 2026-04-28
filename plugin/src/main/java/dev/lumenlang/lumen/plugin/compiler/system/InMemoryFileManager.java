package dev.lumenlang.lumen.plugin.compiler.system;

import org.jetbrains.annotations.NotNull;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.util.HashMap;
import java.util.Map;

/**
 * File manager that stores compiled class bytecode in memory instead of writing to disk.
 *
 * <p>Acts as a wrapper around a standard file manager and intercepts all class file
 * output operations, capturing the compiled bytecode in the {@code classes} map.
 */
public final class InMemoryFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

    /**
     * Map storing compiled class bytecode keyed by fully-qualified class name.
     */
    @NotNull
    public final Map<String, byte[]> classes = new HashMap<>();

    /**
     * Creates an in-memory file manager wrapping the given standard file manager.
     *
     * @param fm the standard file manager to delegate filesystem operations to
     */
    public InMemoryFileManager(@NotNull StandardJavaFileManager fm) {
        super(fm);
    }

    @Override
    @NotNull
    public JavaFileObject getJavaFileForOutput(
            Location location,
            @NotNull String className,
            JavaFileObject.Kind kind,
            FileObject sibling
    ) {
        return new ClassFile(className, classes);
    }

    /**
     * No-op: the delegated {@link StandardJavaFileManager} is managed externally
     * (cached per thread) and must not be closed here.
     */
    @Override
    public void close() {
    }
}
