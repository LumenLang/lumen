package dev.lumenlang.lumen.pipeline.codegen;

import dev.lumenlang.lumen.api.codegen.CodegenContext;
import dev.lumenlang.lumen.pipeline.java.compiled.ClassBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holds per-script class-level metadata that is shared across all handlers during code generation.
 *
 * <p>A single {@code CodegenContextImpl} is created for each script file being compiled. It tracks:
 * <ul>
 *   <li>The generated Java class name, derived from the script file name.</li>
 *   <li>The set of fully-qualified type imports that handlers have requested via
 *       {@link #addImport(String)}.</li>
 * </ul>
 *
 * <p>Handlers that emit code referencing types not in the default import list must call
 * {@link #addImport(String)} to ensure the generated class compiles correctly.
 *
 * @see ClassBuilder
 */
public final class CodegenContextImpl implements CodegenContext {

    private final String scriptName;
    private final String className;
    private final Set<String> imports = new LinkedHashSet<>();
    private final List<String> fieldLines = new ArrayList<>();
    private final List<String> interfaces = new ArrayList<>();
    private final List<String> methodLines = new ArrayList<>();
    private final AtomicInteger methodIdCounter = new AtomicInteger(0);
    private boolean rawJavaEnabled = false;

    /**
     * Creates a new {@code CodegenContextImpl} for the given script file name.
     *
     * <p>The class name is normalised from the script name via
     * {@link ClassBuilder#normalize(String)}.
     *
     * @param scriptName the raw script file name (e.g. {@code "hello.luma"})
     */
    public CodegenContextImpl(@NotNull String scriptName) {
        this.scriptName = scriptName;
        this.className = ClassBuilder.normalize(scriptName);
    }

    /**
     * Returns whether raw Java blocks are enabled for this compilation.
     *
     * @return {@code true} if raw Java blocks are allowed
     */
    public boolean rawJavaEnabled() {
        return rawJavaEnabled;
    }

    /**
     * Sets whether raw Java blocks are enabled for this compilation.
     *
     * @param enabled {@code true} to allow raw Java blocks
     */
    public void setRawJavaEnabled(boolean enabled) {
        this.rawJavaEnabled = enabled;
    }

    /**
     * Returns the raw script file name as passed to the constructor.
     *
     * @return the original script file name (e.g. {@code "hello.luma"})
     */
    @Override
    public @NotNull String scriptName() {
        return scriptName;
    }

    /**
     * Returns the normalised Java class name for the script being compiled.
     *
     * @return the generated class name (e.g. {@code "MyScript"})
     */
    @Override
    public @NotNull String className() {
        return className;
    }

    /**
     * Adds a fully-qualified class name to the import set for the generated script class.
     *
     * <p>Duplicate entries are silently ignored. The formatted {@code import} statement is built
     * internally so callers should pass only the class name itself.
     *
     * @param fqcn the fully-qualified class name to import (e.g. {@code "org.bukkit.inventory.ItemStack"})
     */
    @Override
    public synchronized void addImport(@NotNull String fqcn) {
        imports.add("import " + fqcn + ";");
    }

    /**
     * Returns the complete set of {@code import} statements lines accumulated so far.
     *
     * @return the set of import lines
     */
    public @NotNull Collection<String> importLines() {
        return imports;
    }

    /**
     * Adds a class-level field declaration to the generated script class.
     *
     * @param line the full field declaration line (e.g. {@code "private String name = \"hello\";"})
     */
    @Override
    public synchronized void addField(@NotNull String line) {
        if (!fieldLines.contains(line)) {
            fieldLines.add(line);
        }
    }

    /**
     * Returns an unmodifiable list of all registered field declaration lines.
     *
     * @return the field declaration lines
     */
    public @NotNull List<String> fieldLines() {
        return Collections.unmodifiableList(fieldLines);
    }

    /**
     * Adds an additional interface that the generated class should implement.
     *
     * @param fqcn the simple or fully-qualified interface name
     */
    @Override
    public synchronized void addInterface(@NotNull String fqcn) {
        if (!interfaces.contains(fqcn)) {
            interfaces.add(fqcn);
        }
    }

    /**
     * Returns an unmodifiable list of all registered additional interfaces.
     *
     * @return the interface names
     */
    public @NotNull List<String> interfaces() {
        return Collections.unmodifiableList(interfaces);
    }

    @Override
    public synchronized void addMethod(@NotNull String methodSource) {
        methodLines.add(methodSource);
    }

    public int nextMethodId() {
        return methodIdCounter.getAndIncrement();
    }

    /**
     * Returns an unmodifiable list of all registered method source blocks.
     *
     * @return the method source lines
     */
    public @NotNull List<String> methodLines() {
        return Collections.unmodifiableList(methodLines);
    }
}
