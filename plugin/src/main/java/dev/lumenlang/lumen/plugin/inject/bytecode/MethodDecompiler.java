package dev.lumenlang.lumen.plugin.inject.bytecode;

import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.java.decompiler.main.Fernflower;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.extern.IResultSaver;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decompiles {@code __inject_} method bodies using Vineflower and merges them
 * back into the generated Java source, replacing empty stubs with real code.
 */
public final class MethodDecompiler {

    private static final Pattern STUB_BLOCK = Pattern.compile("([ \\t]*)(// Injected from: [^\\n]+)\\n\\1(private static \\S+ (__inject_\\w+)\\([^)]*\\) \\{[^}]*})");
    private static final String MINI_CLASS_INTERNAL = "lumen/InjectStub";

    /**
     * Replaces empty inject stubs in the given source with decompiled method bodies.
     *
     * @param generatedSource the generated Java source containing stubs
     * @param classBytes      the post injection class bytecodes
     * @return the source with stubs replaced, or the original if nothing was decompiled
     */
    public static @NotNull String rewriteSource(@NotNull String generatedSource, byte @NotNull [] classBytes) {
        Map<String, StubMatch> stubs = findStubs(generatedSource);
        if (stubs.isEmpty()) return generatedSource;

        byte[] miniClass = buildMiniClass(classBytes, stubs.keySet());
        if (miniClass == null) return generatedSource;

        String decompiled = decompile(miniClass);
        if (decompiled == null) return generatedSource;

        Map<String, String> bodies = extractMethods(decompiled, stubs);
        if (bodies.isEmpty()) return generatedSource;

        String result = generatedSource;
        for (Map.Entry<String, String> entry : bodies.entrySet()) {
            StubMatch stub = stubs.get(entry.getKey());
            if (stub == null) continue;
            String replacement = stub.indent + stub.comment + "\n" + stub.indent + "// [Decompiled]\n" + reindent(entry.getValue(), stub.indent);
            result = result.replace(stub.fullMatch, replacement);
        }
        return result;
    }

    /**
     * Builds a minimal class containing only the requested methods from the original class.
     *
     * @param classBytes  the original class bytecodes
     * @param methodNames the set of method names to include
     * @return the minimal class bytecodes, or null on failure
     */
    private static byte @Nullable [] buildMiniClass(byte @NotNull [] classBytes, @NotNull Set<String> methodNames) {
        try {
            ClassReader reader = new ClassReader(classBytes);
            ClassNode original = new ClassNode();
            reader.accept(original, 0);

            ClassNode mini = new ClassNode();
            mini.version = original.version;
            mini.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL;
            mini.name = MINI_CLASS_INTERNAL;
            mini.superName = "java/lang/Object";

            for (MethodNode mn : original.methods) {
                if (methodNames.contains(mn.name)) mini.methods.add(mn);
            }

            if (mini.methods.isEmpty()) return null;

            ClassWriter writer = new ClassWriter(0);
            mini.accept(writer);
            return writer.toByteArray();
        } catch (Exception e) {
            LumenLogger.debug("MethodDecompiler", "Failed to build mini class: " + e.getMessage());
            return null;
        }
    }

    /**
     * Locates all {@code __inject_} stub blocks in the source, keyed by method name.
     *
     * @param source the generated source to scan
     * @return a map of method name to its matched stub
     */
    private static @NotNull Map<String, StubMatch> findStubs(@NotNull String source) {
        Map<String, StubMatch> stubs = new HashMap<>();
        Matcher matcher = STUB_BLOCK.matcher(source);
        while (matcher.find()) {
            stubs.put(matcher.group(4), new StubMatch(matcher.group(0), matcher.group(1), matcher.group(2)));
        }
        return stubs;
    }

    /**
     * Decompiles the given class bytecodes using Vineflower.
     *
     * @param classBytes the compiled class bytes
     * @return the decompiled source, or null on failure
     */
    private static @Nullable String decompile(byte @NotNull [] classBytes) {
        try {
            AtomicReference<String> result = new AtomicReference<>();

            Map<String, Object> options = new HashMap<>();
            options.put(IFernflowerPreferences.HIDE_DEFAULT_CONSTRUCTOR, "0");
            options.put(IFernflowerPreferences.INDENT_STRING, "    ");
            options.put(IFernflowerPreferences.DECOMPILER_COMMENTS, "0");
            options.put(IFernflowerPreferences.DUMP_BYTECODE_ON_ERROR, "0");
            options.put(IFernflowerPreferences.DUMP_EXCEPTION_ON_ERROR, "0");

            IResultSaver saver = new NoOpResultSaver(result);
            IFernflowerLogger logger = new SilentLogger();

            Fernflower fernflower = new Fernflower(saver, options, logger);
            String internalName = MethodDecompiler.MINI_CLASS_INTERNAL.replace('.', '/');
            fernflower.addSource(new InMemorySource(classBytes, internalName));
            fernflower.decompileContext();

            return result.get();
        } catch (Exception e) {
            LumenLogger.debug("MethodDecompiler", "Decompilation failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extracts the full method bodies for each stub from the decompiled source.
     *
     * @param decompiled the full decompiled class source
     * @param stubs      the stub matches to look for
     * @return a map of method name to decompiled method text
     */
    private static @NotNull Map<String, String> extractMethods(@NotNull String decompiled, @NotNull Map<String, StubMatch> stubs) {
        Map<String, String> result = new HashMap<>();
        String[] lines = decompiled.split("\n");
        for (String name : stubs.keySet()) {
            int startLine = -1;
            for (int i = 0; i < lines.length; i++) {
                String trimmed = lines[i].trim();
                if (trimmed.startsWith("private static") && trimmed.contains(" " + name + "(")) {
                    startLine = i;
                    break;
                }
            }
            if (startLine < 0) continue;

            int braceDepth = 0;
            boolean foundOpen = false;
            StringBuilder body = new StringBuilder();
            for (int i = startLine; i < lines.length; i++) {
                if (i > startLine) body.append("\n");
                body.append(lines[i]);
                braceDepth += braceChange(lines[i]);
                if (braceDepth > 0) foundOpen = true;
                if (foundOpen && braceDepth <= 0) break;
            }
            result.put(name, body.toString());
        }
        return result;
    }

    /**
     * Computes the net brace depth change for a single line, ignoring braces inside strings.
     *
     * @param line the source line
     * @return positive for net opening braces, negative for net closing braces
     */
    private static int braceChange(@NotNull String line) {
        int depth = 0;
        boolean inString = false;
        boolean inChar = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\' && (inString || inChar) && i + 1 < line.length()) {
                i++;
                continue;
            }
            if (c == '"' && !inChar) inString = !inString;
            else if (c == '\'' && !inString) inChar = !inChar;
            else if (!inString && !inChar) {
                if (c == '{') depth++;
                else if (c == '}') depth--;
            }
        }
        return depth;
    }

    /**
     * Re-indents a method block so every line starts at the given base indentation.
     *
     * @param method     the raw method text
     * @param baseIndent the whitespace prefix to apply
     * @return the re-indented method text
     */
    private static @NotNull String reindent(@NotNull String method, @NotNull String baseIndent) {
        String[] lines = method.split("\n");
        if (lines.length == 0) return method;

        int minIndent = Integer.MAX_VALUE;
        for (String line : lines) {
            if (line.trim().isEmpty()) continue;
            int spaces = 0;
            for (int i = 0; i < line.length() && line.charAt(i) == ' '; i++) spaces++;
            minIndent = Math.min(minIndent, spaces);
        }
        if (minIndent == Integer.MAX_VALUE) minIndent = 0;

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) result.append("\n");
            if (!lines[i].trim().isEmpty()) {
                result.append(baseIndent).append(lines[i].substring(Math.min(minIndent, lines[i].length())));
            }
        }
        return result.toString();
    }

    private record StubMatch(@NotNull String fullMatch, @NotNull String indent, @NotNull String comment) {
    }

    private record InMemorySource(byte[] classBytes, String internalName) implements IContextSource {

        private InMemorySource(byte @NotNull [] classBytes, @NotNull String internalName) {
            this.classBytes = classBytes;
            this.internalName = internalName;
        }

        @Override
        public @NotNull String getName() {
            return "lumen-inject";
        }

        @Override
        public @NotNull Entries getEntries() {
            return new Entries(List.of(Entry.atBase(internalName + ".class")), List.of(), List.of());
        }

        @Override
        public @NotNull InputStream getInputStream(@NotNull String path) {
            return new ByteArrayInputStream(classBytes);
        }

        @Override
        public @NotNull IOutputSink createOutputSink(@NotNull IResultSaver saver) {
            return new IOutputSink() {
                @Override
                public void begin() {
                }

                @Override
                public void acceptClass(@NotNull String qualifiedName, @NotNull String fileName, @NotNull String content, int @NotNull [] mapping) {
                    saver.saveClassFile(null, qualifiedName, fileName, content, mapping);
                }

                @Override
                public void acceptDirectory(@NotNull String directory) {
                }

                @Override
                public void acceptOther(@NotNull String path) {
                }

                @Override
                public void close() {
                }
            };
        }
    }

    private record NoOpResultSaver(AtomicReference<String> result) implements IResultSaver {

        private NoOpResultSaver(@NotNull AtomicReference<String> result) {
            this.result = result;
        }

        @Override
        public void saveFolder(@NotNull String path) {
        }

        @Override
        public void copyFile(@NotNull String source, @NotNull String path, @NotNull String entryName) {
        }

        @Override
        public void createArchive(@NotNull String path, @NotNull String archiveName, @Nullable Manifest manifest) {
        }

        @Override
        public void saveDirEntry(@NotNull String path, @NotNull String archiveName, @NotNull String entryName) {
        }

        @Override
        public void copyEntry(@NotNull String source, @NotNull String path, @NotNull String archiveName, @NotNull String entry) {
        }

        @Override
        public void saveClassEntry(@NotNull String path, @NotNull String archiveName, @NotNull String qualifiedName, @NotNull String entryName, @NotNull String content) {
        }

        @Override
        public void closeArchive(@NotNull String path, @NotNull String archiveName) {
        }

        @Override
        public void saveClassFile(@Nullable String path, @NotNull String qualifiedName, @NotNull String entryName, @NotNull String content, int @NotNull [] mapping) {
            result.set(content);
        }

        @Override
        public void close() {
        }
    }

    private static final class SilentLogger extends IFernflowerLogger {

        @Override
        public void writeMessage(@NotNull String message, @NotNull Severity severity) {
        }

        @Override
        public void writeMessage(@NotNull String message, @NotNull Severity severity, @Nullable Throwable t) {
        }
    }
}
