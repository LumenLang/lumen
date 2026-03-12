package dev.lumenlang.lumen.pipeline.java.compiler.system;

import org.jetbrains.annotations.NotNull;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;

/**
 * Represents a Java source file stored in memory.
 *
 * <p>Used during compilation to provide source code without reading from disk.
 * The source is identified by a synthetic URI of the form {@code string:///path/to/ClassName.java}.
 */
public final class SourceFile extends SimpleJavaFileObject {

    private final String code;

    /**
     * Creates a source file with the given class name and source code.
     *
     * @param name the fully-qualified class name (e.g., {@code "com.example.MyClass"})
     * @param code the Java source code
     */
    public SourceFile(@NotNull String name, @NotNull String code) {
        super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
        this.code = code;
    }

    @Override
    @NotNull
    public CharSequence getCharContent(boolean ignore) {
        return code;
    }
}
