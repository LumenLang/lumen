package dev.lumenlang.build.scan;

import dev.lumenlang.build.scan.handler.HandlerKind;
import dev.lumenlang.build.scan.handler.HandlerMeta;
import dev.lumenlang.build.scan.handler.ScannedHandler;
import dev.lumenlang.build.scan.param.InjectParam;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Walks {@code .class} files under a classes root and extracts every method
 * annotated with a Lumen handler-kind annotation. Returns a flat list of
 * {@link ScannedHandler} that downstream phases consume.
 */
public final class ClassScanner {

    private static final String ANNOTATION_PKG = "Ldev/lumenlang/lumen/api/pattern/annotation/";
    private static final String STATEMENT_DESC = ANNOTATION_PKG + "Statement;";
    private static final String EXPRESSION_DESC = ANNOTATION_PKG + "Expression;";
    private static final String CONDITION_DESC = ANNOTATION_PKG + "Condition;";
    private static final String PATTERN_DESC = ANNOTATION_PKG + "Pattern;";
    private static final String PATTERNS_DESC = ANNOTATION_PKG + "Patterns;";
    private static final String INJECT_DESC = ANNOTATION_PKG + "Inject;";
    private static final String DESCRIPTION_DESC = ANNOTATION_PKG + "Description;";
    private static final String EXAMPLE_DESC = ANNOTATION_PKG + "Example;";
    private static final String EXAMPLES_DESC = ANNOTATION_PKG + "Examples;";
    private static final String SINCE_DESC = ANNOTATION_PKG + "Since;";
    private static final String CATEGORY_DESC = ANNOTATION_PKG + "Category;";
    private static final String DEPRECATED_DESC = "Ljava/lang/Deprecated;";
    private static final String METHOD_BASED_DESC = ANNOTATION_PKG + "MethodBased;";

    private ClassScanner() {
    }

    public static @NotNull List<ScannedHandler> scan(@NotNull Path classesRoot) throws IOException {
        if (!Files.isDirectory(classesRoot)) return List.of();
        List<ScannedHandler> all = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(classesRoot)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".class"))
                    .sorted(Comparator.naturalOrder())
                    .forEach(p -> {
                        try {
                            scanFile(p, all);
                        } catch (IOException e) {
                            throw new UncheckedScanException(p, e);
                        }
                    });
        } catch (UncheckedScanException e) {
            throw e.ioCause();
        }
        return all;
    }

    private static void scanFile(@NotNull Path classFile, @NotNull List<ScannedHandler> out) throws IOException {
        ClassNode node = new ClassNode();
        try (InputStream in = Files.newInputStream(classFile)) {
            new ClassReader(in).accept(node, ClassReader.SKIP_FRAMES);
        }
        if (node.methods == null) return;
        for (MethodNode method : node.methods) {
            ScannedHandler scanned = scanMethod(node, classFile, method);
            if (scanned != null) out.add(scanned);
        }
    }

    private static @Nullable ScannedHandler scanMethod(@NotNull ClassNode owner, @NotNull Path classFile, @NotNull MethodNode method) {
        HandlerKind kind = kindOf(method);
        if (kind == null) return null;

        List<String> patterns = collectPatterns(method);
        if (patterns.isEmpty()) return null;

        List<InjectParam> injects = collectInjectParams(method);
        HandlerMeta meta = collectMeta(method);
        boolean methodBased = hasAnnotation(method, METHOD_BASED_DESC);

        return new ScannedHandler(owner.name, classFile, method.name, method.desc, kind, patterns, injects, meta, methodBased);
    }

    private static boolean hasAnnotation(@NotNull MethodNode method, @NotNull String desc) {
        if (method.visibleAnnotations == null) return false;
        for (AnnotationNode a : method.visibleAnnotations) {
            if (desc.equals(a.desc)) return true;
        }
        return false;
    }

    private static @Nullable HandlerKind kindOf(@NotNull MethodNode method) {
        if (method.visibleAnnotations == null) return null;
        for (AnnotationNode a : method.visibleAnnotations) {
            if (STATEMENT_DESC.equals(a.desc)) return HandlerKind.STATEMENT;
            if (EXPRESSION_DESC.equals(a.desc)) return HandlerKind.EXPRESSION;
            if (CONDITION_DESC.equals(a.desc)) return HandlerKind.CONDITION;
        }
        return null;
    }

    private static @NotNull List<String> collectPatterns(@NotNull MethodNode method) {
        if (method.visibleAnnotations == null) return List.of();
        List<String> out = new ArrayList<>();
        for (AnnotationNode a : method.visibleAnnotations) {
            if (PATTERN_DESC.equals(a.desc)) {
                String value = stringValue(a, "value");
                if (value != null) out.add(value);
            } else if (PATTERNS_DESC.equals(a.desc)) {
                List<AnnotationNode> nested = arrayValue(a, "value");
                for (AnnotationNode n : nested) {
                    String value = stringValue(n, "value");
                    if (value != null) out.add(value);
                }
            }
        }
        return Collections.unmodifiableList(out);
    }

    private static @NotNull List<InjectParam> collectInjectParams(@NotNull MethodNode method) {
        Type[] argTypes = Type.getArgumentTypes(method.desc);
        if (argTypes.length == 0) return List.of();
        List<AnnotationNode>[] paramAnnotations = method.visibleParameterAnnotations;
        if (paramAnnotations == null) return List.of();

        boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
        int slot = isStatic ? 0 : 1;
        List<InjectParam> out = new ArrayList<>();

        for (int i = 0; i < argTypes.length; i++) {
            List<AnnotationNode> ann = i < paramAnnotations.length ? paramAnnotations[i] : null;
            AnnotationNode inject = findAnnotation(ann, INJECT_DESC);
            if (inject != null) {
                String overrideName = stringValue(inject, "value");
                if (overrideName != null && overrideName.isEmpty()) overrideName = null;
                String sourceName = paramName(method, i, isStatic);
                if (sourceName == null) sourceName = "arg" + i;
                out.add(new InjectParam(sourceName, overrideName, argTypes[i].getDescriptor(), slot));
            }
            slot += argTypes[i].getSize();
        }
        return Collections.unmodifiableList(out);
    }

    private static @Nullable String paramName(@NotNull MethodNode method, int paramIndex, boolean isStatic) {
        if (method.parameters != null && paramIndex < method.parameters.size()) {
            String name = method.parameters.get(paramIndex).name;
            if (name != null) return name;
        }
        if (method.localVariables != null) {
            int targetSlot = isStatic ? paramIndex : paramIndex + 1;
            for (LocalVariableNode lv : method.localVariables) {
                if (lv.index == targetSlot) return lv.name;
            }
        }
        return null;
    }

    private static @NotNull HandlerMeta collectMeta(@NotNull MethodNode method) {
        if (method.visibleAnnotations == null) return HandlerMeta.EMPTY;
        String description = null;
        List<String> examples = new ArrayList<>();
        String since = null;
        String category = null;
        boolean deprecated = false;

        for (AnnotationNode a : method.visibleAnnotations) {
            if (DESCRIPTION_DESC.equals(a.desc)) {
                description = stringValue(a, "value");
            } else if (EXAMPLE_DESC.equals(a.desc)) {
                String value = stringValue(a, "value");
                if (value != null) examples.add(value);
            } else if (EXAMPLES_DESC.equals(a.desc)) {
                List<AnnotationNode> nested = arrayValue(a, "value");
                for (AnnotationNode n : nested) {
                    String value = stringValue(n, "value");
                    if (value != null) examples.add(value);
                }
            } else if (SINCE_DESC.equals(a.desc)) {
                since = stringValue(a, "value");
            } else if (CATEGORY_DESC.equals(a.desc)) {
                category = stringValue(a, "value");
            } else if (DEPRECATED_DESC.equals(a.desc)) {
                deprecated = true;
            }
        }
        return new HandlerMeta(description, List.copyOf(examples), since, category, deprecated);
    }

    private static @Nullable AnnotationNode findAnnotation(@Nullable List<AnnotationNode> list, @NotNull String desc) {
        if (list == null) return null;
        for (AnnotationNode a : list) {
            if (desc.equals(a.desc)) return a;
        }
        return null;
    }

    private static @Nullable String stringValue(@NotNull AnnotationNode a, @NotNull String key) {
        if (a.values == null) return null;
        for (int i = 0; i < a.values.size(); i += 2) {
            if (key.equals(a.values.get(i)) && a.values.get(i + 1) instanceof String s) return s;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static @NotNull List<AnnotationNode> arrayValue(@NotNull AnnotationNode a, @NotNull String key) {
        if (a.values == null) return List.of();
        for (int i = 0; i < a.values.size(); i += 2) {
            if (key.equals(a.values.get(i)) && a.values.get(i + 1) instanceof List<?> list) {
                List<AnnotationNode> out = new ArrayList<>(list.size());
                for (Object o : list) {
                    if (o instanceof AnnotationNode n) out.add(n);
                }
                return out;
            }
        }
        return List.of();
    }
}
