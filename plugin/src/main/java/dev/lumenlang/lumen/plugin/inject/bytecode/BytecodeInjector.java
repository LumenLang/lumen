package dev.lumenlang.lumen.plugin.inject.bytecode;

import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import org.jetbrains.annotations.NotNull;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Performs post-compilation bytecode injection on script classes.
 */
public final class BytecodeInjector {

    private static final Set<String> BOXING_OWNERS = Set.of("java/lang/Integer", "java/lang/Double", "java/lang/Long", "java/lang/Float", "java/lang/Boolean", "java/lang/Byte", "java/lang/Short", "java/lang/Character");

    /**
     * Processes all bytecodes in the given map, applying bytecode injection where needed.
     *
     * @param bytecodes the map of fully qualified class name to bytecode, modified in place
     */
    public static void inject(@NotNull Map<String, byte[]> bytecodes) {
        for (Map.Entry<String, byte[]> entry : bytecodes.entrySet()) {
            String className = entry.getKey();
            List<InjectableMethod> methods = InjectableRegistry.methods(className);
            if (methods == null || methods.isEmpty()) continue;

            byte[] transformed = injectMethods(entry.getValue(), methods);
            entry.setValue(transformed);
            InjectableRegistry.clear(className);
            LumenLogger.debug("BytecodeInjector", "Injected " + methods.size() + " method(s) into " + className);
        }
    }

    private static byte @NotNull [] injectMethods(byte @NotNull [] originalBytecode, @NotNull List<InjectableMethod> methods) {
        ClassReader reader = new ClassReader(originalBytecode);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);

        Map<String, InjectableMethod> methodMap = new HashMap<>();
        for (InjectableMethod m : methods) {
            methodMap.put(m.methodName(), m);
        }

        for (MethodNode mn : classNode.methods) {
            InjectableMethod injectable = methodMap.get(mn.name);
            if (injectable == null) continue;
            replaceMethodBody(mn, injectable, classNode.name);
        }

        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static void replaceMethodBody(@NotNull MethodNode target, @NotNull InjectableMethod injectable, @NotNull String targetClassInternal) {
        ExtractedBody body = injectable.extractedBody();
        int targetReturnOpcode = returnOpcodeForType(injectable.returnType());

        Map<String, Integer> bindingToParam = new HashMap<>();
        int paramSlotCount = 0;
        for (int i = 0; i < injectable.parameterBindings().size(); i++) {
            bindingToParam.put(injectable.parameterBindings().get(i), paramSlotCount);
            String type = injectable.parameterTypes().get(i);
            paramSlotCount += ("long".equals(type) || "double".equals(type)) ? 2 : 1;
        }

        Map<LabelNode, LabelNode> labelMap = new HashMap<>();
        for (AbstractInsnNode insn : body.instructions()) {
            if (insn instanceof LabelNode label) {
                labelMap.put(label, new LabelNode());
            }
        }

        InsnList newBody = new InsnList();
        for (AbstractInsnNode insn : body.instructions()) {
            if (insn instanceof LineNumberNode) continue;

            if (insn.getType() == AbstractInsnNode.METHOD_INSN) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                if (BytecodeExtractor.isFakeCall(methodInsn)) {
                    if (insn.getPrevious() instanceof LdcInsnNode ldc && ldc.cst instanceof String bindingName) {
                        Integer paramIdx = bindingToParam.get(bindingName);
                        if (paramIdx != null) {
                            newBody.add(new VarInsnNode(loadOpcodeForDescriptor(Type.getReturnType(methodInsn.desc).getDescriptor()), paramIdx));
                            continue;
                        }
                    }
                }

                if (isBoxingCall(methodInsn) && targetReturnOpcode != Opcodes.ARETURN) {
                    AbstractInsnNode next = insn.getNext();
                    while (next != null && (next instanceof LabelNode || next.getOpcode() == -1)) next = next.getNext();
                    if (next != null && next.getOpcode() >= Opcodes.IRETURN && next.getOpcode() <= Opcodes.RETURN)
                        continue;
                }

                if (methodInsn.owner.equals(body.sourceClass())) {
                    newBody.add(new MethodInsnNode(methodInsn.getOpcode(), targetClassInternal, methodInsn.name, methodInsn.desc, methodInsn.itf));
                    continue;
                }
            }

            if (insn instanceof LdcInsnNode) {
                AbstractInsnNode next = insn.getNext();
                if (next instanceof MethodInsnNode nextMethod && BytecodeExtractor.isFakeCall(nextMethod)) {
                    continue;
                }
            }

            if (insn.getOpcode() >= Opcodes.IRETURN && insn.getOpcode() <= Opcodes.RETURN) {
                newBody.add(new InsnNode(targetReturnOpcode));
                continue;
            }

            AbstractInsnNode cloned = insn.clone(labelMap);
            if (cloned instanceof VarInsnNode varInsn) {
                varInsn.var += paramSlotCount;
            } else if (cloned instanceof IincInsnNode iinc) {
                iinc.var += paramSlotCount;
            }
            newBody.add(cloned);
        }

        target.instructions.clear();
        target.instructions.add(newBody);
        target.tryCatchBlocks.clear();
        for (TryCatchBlockNode tcb : body.tryCatchBlocks()) {
            LabelNode start = labelMap.getOrDefault(tcb.start, tcb.start);
            LabelNode end = labelMap.getOrDefault(tcb.end, tcb.end);
            LabelNode handler = labelMap.getOrDefault(tcb.handler, tcb.handler);
            target.tryCatchBlocks.add(new TryCatchBlockNode(start, end, handler, tcb.type));
        }
        if (target.localVariables == null) target.localVariables = new ArrayList<>();
        else target.localVariables.clear();
        LabelNode paramStart = new LabelNode();
        LabelNode paramEnd = new LabelNode();
        target.instructions.insert(paramStart);
        target.instructions.add(paramEnd);
        int slot = 0;
        for (int i = 0; i < injectable.parameterBindings().size(); i++) {
            String type = injectable.parameterTypes().get(i);
            target.localVariables.add(new LocalVariableNode(injectable.parameterBindings().get(i), Type.getType(typeToDescriptor(type)).getDescriptor(), null, paramStart, paramEnd, slot));
            slot += ("long".equals(type) || "double".equals(type)) ? 2 : 1;
        }
        for (LocalVariableNode lv : body.localVariables()) {
            LabelNode start = labelMap.getOrDefault(lv.start, lv.start);
            LabelNode end = labelMap.getOrDefault(lv.end, lv.end);
            target.localVariables.add(new LocalVariableNode(lv.name, lv.desc, lv.signature, start, end, lv.index + paramSlotCount));
        }
        target.maxStack = Math.max(body.maxStack(), 4);
        target.maxLocals = body.maxLocals() + paramSlotCount;
    }

    private static boolean isBoxingCall(@NotNull MethodInsnNode insn) {
        if (insn.getOpcode() != Opcodes.INVOKESTATIC || !"valueOf".equals(insn.name)) return false;
        return BOXING_OWNERS.contains(insn.owner);
    }

    private static int returnOpcodeForType(@NotNull String type) {
        return switch (type) {
            case "int", "boolean", "byte", "short", "char" -> Opcodes.IRETURN;
            case "long" -> Opcodes.LRETURN;
            case "float" -> Opcodes.FRETURN;
            case "double" -> Opcodes.DRETURN;
            case "void" -> Opcodes.RETURN;
            default -> Opcodes.ARETURN;
        };
    }

    private static int loadOpcodeForDescriptor(@NotNull String descriptor) {
        return switch (descriptor) {
            case "I", "Z" -> Opcodes.ILOAD;
            case "J" -> Opcodes.LLOAD;
            case "F" -> Opcodes.FLOAD;
            case "D" -> Opcodes.DLOAD;
            default -> Opcodes.ALOAD;
        };
    }

    private static @NotNull String typeToDescriptor(@NotNull String javaType) {
        return switch (javaType) {
            case "int" -> "I";
            case "long" -> "J";
            case "double" -> "D";
            case "float" -> "F";
            case "boolean" -> "Z";
            case "byte" -> "B";
            case "short" -> "S";
            case "char" -> "C";
            case "void" -> "V";
            default -> "L" + javaType.replace('.', '/') + ";";
        };
    }
}
