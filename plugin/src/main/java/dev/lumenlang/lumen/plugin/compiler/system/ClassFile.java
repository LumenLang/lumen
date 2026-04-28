package dev.lumenlang.lumen.plugin.compiler.system;

import org.jetbrains.annotations.NotNull;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;

/**
 * Represents a compiled Java class file stored in memory.
 *
 * <p>When opened for output, captures the compiled bytecode and stores it in a map
 * keyed by the fully-qualified class name. Used during in-memory compilation to
 * capture emitted class bytecode.
 */
public final class ClassFile extends SimpleJavaFileObject {

    private final String name;
    private final Map<String, byte[]> out;

    /**
     * Creates a class file that will store its bytecode in the given map.
     *
     * @param name the fully-qualified class name
     * @param out  the map to store the compiled bytecode in
     */
    public ClassFile(@NotNull String name, @NotNull Map<String, byte[]> out) {
        super(URI.create("mem:///" + name.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
        this.name = name;
        this.out = out;
    }

    @Override
    @NotNull
    public OutputStream openOutputStream() {
        return new ByteArrayOutputStream() {
            @Override
            public void close() {
                out.put(name, toByteArray());
            }
        };
    }
}
