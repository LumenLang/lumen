package dev.lumenlang.build.rewrite;

import dev.lumenlang.build.scan.handler.ScannedHandler;
import dev.lumenlang.build.source.ParsedHandlerSource;
import dev.lumenlang.build.source.phase.Phase;
import dev.lumenlang.build.source.phase.PhaseMarker;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rewrites annotated handler bytecode in place: drops every instruction that
 * resolves to a {@code // lumen:runtime} source line, then appends a return
 * matching the method's declared return type.
 *
 * <p>The remaining bytecode runs only the {@code // lumen:compile} section,
 * which the runtime invokes at codegen so the section's {@code HandlerContext}
 * calls take effect before the runtime body emits.
 */
public final class MethodTrimmer {

    private MethodTrimmer() {
    }

    public static void trim(@NotNull ScannedHandler scanned, @NotNull ParsedHandlerSource parsed) throws IOException {
        if (!scanned.wantsContext()) return;

        Path classFile = scanned.classFile();
        ClassNode node;
        try (InputStream in = Files.newInputStream(classFile)) {
            node = new ClassNode();
            new ClassReader(in).accept(node, 0);
        }

        MethodNode method = findMethod(node, scanned.methodName(), scanned.methodDescriptor());
        if (method == null) return;

        Map<AbstractInsnNode, Integer> insnLines = mapInsnLines(method.instructions);
        rewriteBody(method, insnLines, parsed.markers());

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        Files.write(classFile, writer.toByteArray());
    }

    private static void rewriteBody(@NotNull MethodNode method, @NotNull Map<AbstractInsnNode, Integer> insnLines, @NotNull List<PhaseMarker> markers) {
        List<AbstractInsnNode> snapshot = new ArrayList<>();
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) snapshot.add(insn);

        InsnList kept = new InsnList();
        for (AbstractInsnNode insn : snapshot) {
            if (insn instanceof LabelNode) {
                kept.add(insn);
                continue;
            }
            Integer line = insnLines.get(insn);
            if (line != null && phaseAt(line, markers) == Phase.RUNTIME) continue;
            kept.add(insn);
        }
        appendReturn(kept, Type.getReturnType(method.desc));
        method.instructions = kept;
        method.localVariables = filterLocalVariables(method.localVariables, kept);
        method.tryCatchBlocks = new ArrayList<>();
        method.maxStack = 0;
        method.maxLocals = 0;
    }

    private static List<LocalVariableNode> filterLocalVariables(List<LocalVariableNode> originals, @NotNull InsnList kept) {
        if (originals == null) return null;
        Set<AbstractInsnNode> alive = new HashSet<>();
        for (AbstractInsnNode insn = kept.getFirst(); insn != null; insn = insn.getNext()) alive.add(insn);
        List<LocalVariableNode> out = new ArrayList<>();
        for (LocalVariableNode lv : originals) {
            if (alive.contains(lv.start) && alive.contains(lv.end)) out.add(lv);
        }
        return out;
    }

    private static @NotNull Map<AbstractInsnNode, Integer> mapInsnLines(@NotNull InsnList insns) {
        Map<AbstractInsnNode, Integer> map = new HashMap<>();
        int currentLine = -1;
        for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof LineNumberNode l) currentLine = l.line;
            if (currentLine != -1) map.put(insn, currentLine);
        }
        return map;
    }

    private static @NotNull Phase phaseAt(int line, @NotNull List<PhaseMarker> markers) {
        Phase current = Phase.RUNTIME;
        for (PhaseMarker m : markers) {
            if (m.line() > line) break;
            current = m.phase();
        }
        return current;
    }

    private static void appendReturn(@NotNull InsnList list, @NotNull Type returnType) {
        switch (returnType.getSort()) {
            case Type.VOID -> list.add(new InsnNode(Opcodes.RETURN));
            case Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> {
                list.add(new InsnNode(Opcodes.ICONST_0));
                list.add(new InsnNode(Opcodes.IRETURN));
            }
            case Type.LONG -> {
                list.add(new InsnNode(Opcodes.LCONST_0));
                list.add(new InsnNode(Opcodes.LRETURN));
            }
            case Type.FLOAT -> {
                list.add(new InsnNode(Opcodes.FCONST_0));
                list.add(new InsnNode(Opcodes.FRETURN));
            }
            case Type.DOUBLE -> {
                list.add(new InsnNode(Opcodes.DCONST_0));
                list.add(new InsnNode(Opcodes.DRETURN));
            }
            default -> {
                list.add(new InsnNode(Opcodes.ACONST_NULL));
                list.add(new InsnNode(Opcodes.ARETURN));
            }
        }
    }

    private static MethodNode findMethod(@NotNull ClassNode node, @NotNull String name, @NotNull String descriptor) {
        if (node.methods == null) return null;
        for (MethodNode m : node.methods) {
            if (name.equals(m.name) && descriptor.equals(m.desc)) return m;
        }
        return null;
    }
}
