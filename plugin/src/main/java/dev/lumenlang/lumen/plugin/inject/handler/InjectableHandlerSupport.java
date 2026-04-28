package dev.lumenlang.lumen.plugin.inject.handler;

import dev.lumenlang.lumen.api.codegen.CodegenAccess;
import dev.lumenlang.lumen.api.type.LumenType;
import dev.lumenlang.lumen.plugin.inject.bytecode.ExtractedBody;
import dev.lumenlang.lumen.plugin.inject.bytecode.InjectableMethod;
import dev.lumenlang.lumen.plugin.inject.bytecode.InjectableRegistry;
import dev.lumenlang.lumen.plugin.inject.bytecode.MethodDecompiler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared logic for injectable handlers (statement, expression, condition).
 *
 * <p>Supports two modes: inline (default) where the decompiled body is emitted
 * directly at the call site, and method based where a bridge method is generated
 * and the body is injected via bytecode post compilation.
 */
public final class InjectableHandlerSupport {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([^:]+):[^%]+%");
    private static final Set<String> USED_NAMES = Collections.synchronizedSet(new HashSet<>());

    private final ExtractedBody extractedBody;
    private final String returnType;
    private final boolean methodBased;
    private final Set<CodegenAccess> emittedContexts = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private String methodName;
    private volatile @Nullable MethodDecompiler.DecompiledInlineBody inlineBody;
    private volatile boolean inlineResolved;

    public InjectableHandlerSupport(@NotNull ExtractedBody extractedBody, @NotNull String returnType, boolean methodBased) {
        this.extractedBody = extractedBody;
        this.methodName = deduplicate(generateFallbackName(extractedBody));
        this.returnType = returnType;
        this.methodBased = methodBased;
    }

    private static @NotNull String deduplicate(@NotNull String base) {
        if (USED_NAMES.add(base)) return base;
        int i = 2;
        while (!USED_NAMES.add(base + "_" + i)) i++;
        return base + "_" + i;
    }

    private static @NotNull String generatePatternName(@NotNull String pattern) {
        String stripped = PLACEHOLDER_PATTERN.matcher(pattern).replaceAll("").trim();
        String[] words = stripped.split("\\s+");
        StringBuilder name = new StringBuilder("__inject");
        for (String word : words) {
            if (word.isEmpty()) continue;
            String sanitized = word.replaceAll("[^a-zA-Z0-9_]", "");
            if (!sanitized.isEmpty()) name.append('_').append(sanitized);
        }
        return name.toString();
    }

    private static @NotNull String generateFallbackName(@NotNull ExtractedBody body) {
        String source = body.sourceClass();
        int lastSlash = source.lastIndexOf('/');
        String simpleClass = (lastSlash >= 0 ? source.substring(lastSlash + 1) : source).replace('$', '_');
        String method = body.sourceMethodName();
        if (method.startsWith("lambda$")) method = null;
        StringBuilder name = new StringBuilder("__inject_");
        name.append(simpleClass);
        if (method != null) name.append('_').append(method);
        return name.toString();
    }

    public static @NotNull String descriptorToJavaType(@NotNull String descriptor) {
        return switch (descriptor) {
            case "I" -> "int";
            case "J" -> "long";
            case "D" -> "double";
            case "F" -> "float";
            case "Z" -> "boolean";
            case "B" -> "byte";
            case "S" -> "short";
            case "C" -> "char";
            case "V" -> "void";
            default -> {
                Type type = Type.getType(descriptor);
                if (type.getSort() == Type.ARRAY) {
                    Type element = type.getElementType();
                    String base = element.getSort() == Type.OBJECT ? element.getClassName().replace('$', '.') : descriptorToJavaType(element.getDescriptor());
                    yield base + "[]".repeat(type.getDimensions());
                }
                yield type.getSort() == Type.OBJECT ? type.getClassName().replace('$', '.') : "Object";
            }
        };
    }

    public static @NotNull String simpleClassName(@NotNull String fqcn) {
        int lastDot = fqcn.lastIndexOf('.');
        return lastDot >= 0 ? fqcn.substring(lastDot + 1) : fqcn;
    }

    public static @Nullable String descriptorToTypeId(@NotNull String descriptor) {
        String javaType = descriptorToJavaType(descriptor);
        LumenType type = LumenType.fromJavaType(javaType);
        return type != null ? type.id() : null;
    }

    private static @NotNull String defaultReturn(@NotNull String type) {
        return switch (type) {
            case "byte", "short", "char", "int", "long", "float", "double" -> "0";
            case "boolean" -> "false";
            default -> "null";
        };
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

    public void patternHint(@NotNull String pattern) {
        USED_NAMES.remove(methodName);
        this.methodName = deduplicate(generatePatternName(pattern));
        validateBindings(pattern);
    }

    /**
     * Validates bindings for an additional pattern without changing the method name.
     *
     * @param pattern the additional pattern to validate
     */
    public void validateAdditionalPattern(@NotNull String pattern) {
        validateBindings(pattern);
    }

    private void validateBindings(@NotNull String pattern) {
        List<String> patternBindings = new ArrayList<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(pattern);
        while (matcher.find()) patternBindings.add(matcher.group(1));
        for (ExtractedBody.FakeBinding binding : extractedBody.fakeBindings()) {
            if (!patternBindings.contains(binding.bindingName())) {
                throw new IllegalArgumentException("Injectable binding name '" + binding.bindingName() + "' does not match any pattern placeholder. Pattern: \"" + pattern + "\", available placeholders: " + patternBindings);
            }
        }
    }

    public @NotNull String methodName() {
        return methodName;
    }

    /**
     * Returns the decompiled inline body, or null if inline mode is not available.
     */
    public @Nullable MethodDecompiler.DecompiledInlineBody inlineBody() {
        if (methodBased) return null;
        return resolveInlineBody();
    }

    /**
     * Adds imports required by the inline body to the codegen context.
     */
    public void addInlineImports(@NotNull CodegenAccess codegen) {
        MethodDecompiler.DecompiledInlineBody body = inlineBody();
        if (body == null) return;
        for (String imp : body.imports()) {
            if (!imp.startsWith("java.lang.")) codegen.addImport(imp);
        }
        for (ExtractedBody.FakeBinding binding : extractedBody.fakeBindings()) {
            String javaType = descriptorToJavaType(binding.returnDescriptor()).replace("[]", "");
            if (javaType.contains(".") && !javaType.startsWith("java.lang.")) codegen.addImport(javaType);
        }
    }

    /**
     * Replaces binding parameter names in the given line with the actual Java expressions.
     */
    public @NotNull String replaceBindings(@NotNull String line, @NotNull Map<String, String> bindingExpressions) {
        String result = line;
        for (Map.Entry<String, String> entry : bindingExpressions.entrySet()) {
            String paramName = entry.getKey();
            String expression = entry.getValue();
            if (paramName.equals(expression)) continue;
            result = replaceOutsideStrings(result, paramName, expression);
        }
        return result;
    }

    /**
     * Returns the ordered list of fake bindings.
     */
    public @NotNull List<ExtractedBody.FakeBinding> bindings() {
        return extractedBody.fakeBindings();
    }

    private @Nullable MethodDecompiler.DecompiledInlineBody resolveInlineBody() {
        if (inlineResolved) return inlineBody;
        synchronized (this) {
            if (inlineResolved) return inlineBody;
            List<String> paramTypes = new ArrayList<>();
            List<String> paramNames = new ArrayList<>();
            for (ExtractedBody.FakeBinding binding : extractedBody.fakeBindings()) {
                paramTypes.add(descriptorToJavaType(binding.returnDescriptor()));
                paramNames.add(binding.bindingName());
            }
            inlineBody = MethodDecompiler.decompileForInline(extractedBody, methodName, returnType, paramTypes, paramNames);
            inlineResolved = true;
            return inlineBody;
        }
    }

    /**
     * Emits the bridge method and registers the injection if not already done for this codegen context.
     * Returns the parameter binding list for building the call expression.
     * Used in method based mode.
     */
    public @NotNull List<ExtractedBody.FakeBinding> emitIfNeeded(@NotNull CodegenAccess codegen) {
        List<ExtractedBody.FakeBinding> bindings = extractedBody.fakeBindings();

        if (emittedContexts.add(codegen)) {
            synchronized (this) {
                List<String> paramTypes = new ArrayList<>();
                List<String> paramBindings = new ArrayList<>();

                for (ExtractedBody.FakeBinding binding : bindings) {
                    String javaType = descriptorToJavaType(binding.returnDescriptor());
                    String importType = javaType.replace("[]", "");
                    if (importType.contains(".") && !importType.startsWith("java.lang.")) codegen.addImport(importType);
                    paramTypes.add(javaType);
                    paramBindings.add(binding.bindingName());
                }

                String returnTypeImport = returnType.replace("[]", "");
                if (returnTypeImport.contains(".") && !returnTypeImport.startsWith("java.lang."))
                    codegen.addImport(returnTypeImport);

                StringBuilder sig = new StringBuilder();
                sig.append("// Injected from: ").append(extractedBody.sourceClass().replace('/', '.')).append("#").append(extractedBody.sourceMethodName()).append("\n");
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

                InjectableRegistry.register("dev.lumenlang.lumen.java.compiled." + codegen.className(), new InjectableMethod(methodName, returnType, paramTypes, paramBindings, extractedBody));
            }
        }

        return bindings;
    }
}
