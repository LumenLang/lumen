package dev.lumenlang.lumen.pipeline.inject.handler;

import dev.lumenlang.lumen.api.codegen.CodegenAccess;
import dev.lumenlang.lumen.pipeline.inject.bytecode.ExtractedBody;
import dev.lumenlang.lumen.pipeline.inject.bytecode.InjectableMethod;
import dev.lumenlang.lumen.pipeline.inject.bytecode.InjectableRegistry;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared logic for injectable handlers (statement, expression, condition).
 */
final class InjectableHandlerSupport {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private final ExtractedBody extractedBody;
    private final String methodName;
    private final String returnType;
    private final Set<CodegenAccess> emittedContexts = Collections.newSetFromMap(new IdentityHashMap<>());

    InjectableHandlerSupport(@NotNull ExtractedBody extractedBody, @NotNull String returnType) {
        this.extractedBody = extractedBody;
        this.methodName = "__injectable_" + COUNTER.getAndIncrement();
        this.returnType = returnType;
    }

    @NotNull String methodName() {
        return methodName;
    }

    /**
     * Emits the bridge method and registers the injection if not already done for this codegen context.
     * Returns the parameter binding list for building the call expression.
     */
    @NotNull List<ExtractedBody.FakeBinding> emitIfNeeded(@NotNull CodegenAccess codegen) {
        List<ExtractedBody.FakeBinding> bindings = extractedBody.fakeBindings();

        if (emittedContexts.add(codegen)) {
            List<String> paramTypes = new ArrayList<>();
            List<String> paramBindings = new ArrayList<>();

            for (ExtractedBody.FakeBinding binding : bindings) {
                String javaType = descriptorToJavaType(binding.returnDescriptor());
                if (javaType.contains(".") && !javaType.startsWith("java.lang.")) codegen.addImport(javaType);
                paramTypes.add(javaType);
                paramBindings.add(binding.bindingName());
            }

            String returnTypeImport = returnType;
            if (returnTypeImport.contains(".") && !returnTypeImport.startsWith("java.lang.")) codegen.addImport(returnTypeImport);

            StringBuilder sig = new StringBuilder();
            sig.append("// Injected from: ").append(extractedBody.sourceClass().replace('/', '.')).append("\n");
            sig.append("private static ").append(simpleClassName(returnType)).append(" ").append(methodName).append("(");
            for (int i = 0; i < paramTypes.size(); i++) {
                if (i > 0) sig.append(", ");
                sig.append(simpleClassName(paramTypes.get(i))).append(" ").append(paramBindings.get(i));
            }
            sig.append(") {");
            if (!"void".equals(returnType)) {
                sig.append(" return ").append(defaultReturn(returnType)).append(";");
            }
            sig.append(" }");
            codegen.addMethod(sig.toString());

            String fqcn = "dev.lumenlang.lumen.java.compiled." + codegen.className();
            InjectableMethod injectable = new InjectableMethod(methodName, returnType, paramTypes, paramBindings, extractedBody);
            InjectableRegistry.register(fqcn, injectable);
        }

        return bindings;
    }

    static @NotNull String descriptorToJavaType(@NotNull String descriptor) {
        return switch (descriptor) {
            case "I" -> "int";
            case "J" -> "long";
            case "D" -> "double";
            case "F" -> "float";
            case "Z" -> "boolean";
            case "V" -> "void";
            default -> {
                Type type = Type.getType(descriptor);
                yield type.getSort() == Type.OBJECT ? type.getClassName().replace('$', '.') : "Object";
            }
        };
    }

    static @NotNull String simpleClassName(@NotNull String fqcn) {
        int lastDot = fqcn.lastIndexOf('.');
        return lastDot >= 0 ? fqcn.substring(lastDot + 1) : fqcn;
    }

    private static @NotNull String defaultReturn(@NotNull String type) {
        return switch (type) {
            case "int", "long", "float", "double" -> "0";
            case "boolean" -> "false";
            default -> "null";
        };
    }
}
