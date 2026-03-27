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
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private static final Pattern ALIAS_PATTERN = Pattern.compile("^\\s*(\\S+)\\s+(\\w+)\\s*=\\s*(?:\\([^)]*\\)\\s*)?(\\w+)\\s*;\\s*$");

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
     * Decompiles an extracted body into inline-ready source lines and imports.
     *
     * @param body       the extracted bytecode body
     * @param methodName a temporary method name for decompilation
     * @param returnType the Java return type (e.g. "void", "boolean", "org.bukkit.Location")
     * @param paramTypes the Java types of the parameters, ordered by binding occurrence
     * @param paramNames the binding names corresponding to each parameter
     * @return the decompiled inline body, or null if decompilation fails
     */
    public static @Nullable DecompiledInlineBody decompileForInline(@NotNull ExtractedBody body, @NotNull String methodName, @NotNull String returnType, @NotNull List<String> paramTypes, @NotNull List<String> paramNames) {
        try {
            boolean alwaysThrows = true;
            for (AbstractInsnNode insn : body.instructions()) {
                if (insn.getOpcode() >= Opcodes.IRETURN && insn.getOpcode() <= Opcodes.RETURN) {
                    alwaysThrows = false;
                    break;
                }
            }
            byte[] classBytes = buildInjectedStub(body, methodName, returnType, paramTypes, paramNames);
            String decompiled = decompile(classBytes);
            if (decompiled == null) return null;
            return parseDecompiledBody(decompiled, methodName, returnType, paramNames, alwaysThrows);
        } catch (Exception e) {
            LumenLogger.debug("MethodDecompiler", "Inline decompilation failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Builds a stub class containing a single method with the extracted body already injected.
     * Uses COMPUTE_MAXS only (no COMPUTE_FRAMES) to avoid class resolution issues.
     */
    private static byte @NotNull [] buildInjectedStub(@NotNull ExtractedBody body, @NotNull String methodName, @NotNull String returnType, @NotNull List<String> paramTypes, @NotNull List<String> paramNames) {
        ClassNode classNode = new ClassNode();
        classNode.version = Opcodes.V1_8;
        classNode.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL;
        classNode.name = MINI_CLASS_INTERNAL;
        classNode.superName = "java/lang/Object";

        StringBuilder desc = new StringBuilder("(");
        for (String type : paramTypes) desc.append(BytecodeInjector.typeToDescriptor(type));
        desc.append(")").append(BytecodeInjector.typeToDescriptor(returnType));

        MethodNode method = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, methodName, desc.toString(), null, null);

        int targetReturnOpcode = returnOpcodeFor(returnType);

        Map<String, Integer> bindingToSlot = new HashMap<>();
        int paramSlotCount = 0;
        for (int i = 0; i < paramNames.size(); i++) {
            bindingToSlot.put(paramNames.get(i), paramSlotCount);
            paramSlotCount += isWideType(paramTypes.get(i)) ? 2 : 1;
        }

        Map<LabelNode, LabelNode> labelMap = new HashMap<>();
        for (AbstractInsnNode insn : body.instructions()) {
            if (insn instanceof LabelNode label) labelMap.put(label, new LabelNode());
        }

        InsnList newBody = new InsnList();
        for (AbstractInsnNode insn : body.instructions()) {
            if (insn instanceof LineNumberNode) continue;

            if (insn.getType() == AbstractInsnNode.METHOD_INSN) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                if (BytecodeExtractor.isFakeCall(methodInsn)) {
                    if (insn.getPrevious() instanceof LdcInsnNode ldc && ldc.cst instanceof String bindingName) {
                        Integer slot = bindingToSlot.get(bindingName);
                        if (slot != null) {
                            newBody.add(new VarInsnNode(loadOpcodeFor(Type.getReturnType(methodInsn.desc).getDescriptor()), slot));
                            continue;
                        }
                    }
                }
            }

            if (insn instanceof LdcInsnNode) {
                AbstractInsnNode next = insn.getNext();
                if (next instanceof MethodInsnNode nextMethod && BytecodeExtractor.isFakeCall(nextMethod)) continue;
            }

            if (insn.getOpcode() >= Opcodes.IRETURN && insn.getOpcode() <= Opcodes.RETURN) {
                newBody.add(new InsnNode(targetReturnOpcode));
                continue;
            }

            AbstractInsnNode cloned = insn.clone(labelMap);
            if (cloned instanceof VarInsnNode varInsn) varInsn.var += paramSlotCount;
            else if (cloned instanceof IincInsnNode iinc) iinc.var += paramSlotCount;
            newBody.add(cloned);
        }

        method.instructions = newBody;

        method.tryCatchBlocks = new ArrayList<>();
        for (TryCatchBlockNode tcb : body.tryCatchBlocks()) {
            LabelNode start = labelMap.getOrDefault(tcb.start, tcb.start);
            LabelNode end = labelMap.getOrDefault(tcb.end, tcb.end);
            LabelNode handler = labelMap.getOrDefault(tcb.handler, tcb.handler);
            method.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, tcb.type));
        }

        method.localVariables = new ArrayList<>();
        LabelNode paramStart = new LabelNode();
        LabelNode paramEnd = new LabelNode();
        method.instructions.insert(paramStart);
        method.instructions.add(paramEnd);
        int slot = 0;
        for (int i = 0; i < paramNames.size(); i++) {
            method.localVariables.add(new LocalVariableNode(paramNames.get(i), BytecodeInjector.typeToDescriptor(paramTypes.get(i)), null, paramStart, paramEnd, slot));
            slot += isWideType(paramTypes.get(i)) ? 2 : 1;
        }
        for (LocalVariableNode lv : body.localVariables()) {
            LabelNode start = labelMap.getOrDefault(lv.start, lv.start);
            LabelNode end = labelMap.getOrDefault(lv.end, lv.end);
            method.localVariables.add(new LocalVariableNode(lv.name, lv.desc, lv.signature, start, end, lv.index + paramSlotCount));
        }

        method.maxStack = Math.max(body.maxStack(), 4);
        method.maxLocals = body.maxLocals() + paramSlotCount;
        classNode.methods.add(method);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    /**
     * Parses decompiled Vineflower output, folds parameter aliases, and produces inline body lines.
     */
    private static @Nullable DecompiledInlineBody parseDecompiledBody(@NotNull String decompiled, @NotNull String methodName, @NotNull String returnType, @NotNull List<String> paramNames, boolean alwaysThrows) {
        String[] lines = decompiled.split("\n");
        List<String> imports = new ArrayList<>();
        int methodStart = -1;

        for (int i = 0; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (trimmed.startsWith("import ") && trimmed.endsWith(";")) {
                imports.add(trimmed.substring("import ".length(), trimmed.length() - 1).trim());
            }
            if (trimmed.startsWith("private static") && trimmed.contains(" " + methodName + "(")) {
                methodStart = i;
            }
        }
        if (methodStart < 0) return null;

        int braceDepth = 0;
        boolean foundOpen = false;
        int bodyStart = -1;
        int bodyEnd = -1;
        for (int i = methodStart; i < lines.length; i++) {
            int change = braceChange(lines[i]);
            braceDepth += change;
            if (braceDepth > 0 && !foundOpen) {
                foundOpen = true;
                bodyStart = i + 1;
            }
            if (foundOpen && braceDepth <= 0) {
                bodyEnd = i;
                break;
            }
        }
        if (bodyStart < 0 || bodyEnd < 0 || bodyStart >= bodyEnd) return null;

        int minIndent = Integer.MAX_VALUE;
        for (int i = bodyStart; i < bodyEnd; i++) {
            if (lines[i].trim().isEmpty()) continue;
            int spaces = 0;
            for (int j = 0; j < lines[i].length() && lines[i].charAt(j) == ' '; j++) spaces++;
            minIndent = Math.min(minIndent, spaces);
        }
        if (minIndent == Integer.MAX_VALUE) minIndent = 0;

        List<String> bodyLines = new ArrayList<>();
        for (int i = bodyStart; i < bodyEnd; i++) {
            if (lines[i].trim().isEmpty()) continue;
            bodyLines.add(lines[i].substring(Math.min(minIndent, lines[i].length())));
        }
        if (bodyLines.isEmpty()) return null;

        Set<String> paramSet = Set.copyOf(paramNames);
        Map<String, String> aliases = new LinkedHashMap<>();
        List<String> filtered = new ArrayList<>();
        for (String line : bodyLines) {
            Matcher m = ALIAS_PATTERN.matcher(line);
            if (m.matches() && paramSet.contains(m.group(3))) {
                aliases.put(m.group(2), m.group(3));
                continue;
            }
            filtered.add(line);
        }

        if (!aliases.isEmpty()) {
            List<String> folded = new ArrayList<>();
            for (String line : filtered) {
                String result = line;
                for (Map.Entry<String, String> alias : aliases.entrySet()) {
                    result = replaceOutsideStrings(result, alias.getKey(), alias.getValue());
                }
                folded.add(result);
            }
            filtered = folded;
        }
        if (filtered.isEmpty()) return null;

        String returnExpression = null;
        if (!"void".equals(returnType) && filtered.size() == 1) {
            String single = filtered.get(0).trim();
            if (single.startsWith("return ") && single.endsWith(";")) {
                returnExpression = single.substring("return ".length(), single.length() - 1).trim();
            }
        }

        return new DecompiledInlineBody(filtered, imports, returnExpression, alwaysThrows);
    }

    private static int returnOpcodeFor(@NotNull String type) {
        return switch (type) {
            case "int", "boolean", "byte", "short", "char" -> Opcodes.IRETURN;
            case "long" -> Opcodes.LRETURN;
            case "float" -> Opcodes.FRETURN;
            case "double" -> Opcodes.DRETURN;
            case "void" -> Opcodes.RETURN;
            default -> Opcodes.ARETURN;
        };
    }

    private static int loadOpcodeFor(@NotNull String descriptor) {
        return switch (descriptor) {
            case "I", "Z" -> Opcodes.ILOAD;
            case "J" -> Opcodes.LLOAD;
            case "F" -> Opcodes.FLOAD;
            case "D" -> Opcodes.DLOAD;
            default -> Opcodes.ALOAD;
        };
    }

    private static boolean isWideType(@NotNull String type) {
        return "long".equals(type) || "double".equals(type);
    }

    private static @NotNull String replaceOutsideStrings(@NotNull String line, @NotNull String target, @NotNull String replacement) {
        Pattern p = Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'|\\b" + Pattern.quote(target) + "\\b");
        Matcher m = p.matcher(line);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            if (m.group().startsWith("\"") || m.group().startsWith("'")) {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group()));
            } else {
                m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
        }
        m.appendTail(sb);
        return sb.toString();
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

    /**
     * Holds the decompiled inline body of an injectable method.
     *
     * @param bodyLines        the decompiled body lines (without method signature or braces)
     * @param imports          the fully qualified class names that need to be imported
     * @param returnExpression for single return bodies, the return expression without "return" and ";", or null
     */
    public record DecompiledInlineBody(@NotNull List<String> bodyLines, @NotNull List<String> imports,
                                       @Nullable String returnExpression, boolean alwaysThrows) {
    }
}
