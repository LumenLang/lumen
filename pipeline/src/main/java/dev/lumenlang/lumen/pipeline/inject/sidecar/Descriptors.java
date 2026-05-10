package dev.lumenlang.lumen.pipeline.inject.sidecar;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts JVMS descriptors into the Java type names used at codegen time
 * when emitting bridge methods or import lists, plus argument-list parsing
 * used when reflectively invoking trimmed handler methods.
 */
public final class Descriptors {

    private Descriptors() {
    }

    /**
     * Splits a method descriptor's argument list into its component field
     * descriptors, in declaration order.
     */
    public static @NotNull List<String> argumentDescriptors(@NotNull String methodDescriptor) {
        if (!methodDescriptor.startsWith("(")) throw new IllegalArgumentException("Bad method descriptor: " + methodDescriptor);
        int close = methodDescriptor.indexOf(')');
        if (close < 0) throw new IllegalArgumentException("Bad method descriptor: " + methodDescriptor);
        List<String> out = new ArrayList<>();
        int i = 1;
        while (i < close) {
            int start = i;
            while (i < close && methodDescriptor.charAt(i) == '[') i++;
            char c = methodDescriptor.charAt(i);
            if (c == 'L') {
                int semi = methodDescriptor.indexOf(';', i);
                if (semi < 0 || semi >= close) throw new IllegalArgumentException("Bad method descriptor: " + methodDescriptor);
                out.add(methodDescriptor.substring(start, semi + 1));
                i = semi + 1;
            } else {
                out.add(methodDescriptor.substring(start, i + 1));
                i++;
            }
        }
        return out;
    }

    /**
     * Resolves a field descriptor to its runtime {@link Class}, loading
     * reference types through {@code loader}.
     */
    public static @NotNull Class<?> classOf(@NotNull String descriptor, @NotNull ClassLoader loader) throws ClassNotFoundException {
        return switch (descriptor) {
            case "B" -> byte.class;
            case "S" -> short.class;
            case "C" -> char.class;
            case "I" -> int.class;
            case "Z" -> boolean.class;
            case "J" -> long.class;
            case "F" -> float.class;
            case "D" -> double.class;
            case "V" -> void.class;
            default -> {
                if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
                    yield Class.forName(descriptor.substring(1, descriptor.length() - 1).replace('/', '.'), false, loader);
                }
                if (descriptor.startsWith("[")) {
                    yield Class.forName(descriptor.replace('/', '.'), false, loader);
                }
                throw new IllegalArgumentException("Unsupported descriptor: " + descriptor);
            }
        };
    }

    /**
     * Returns the Java source-level type name for a JVMS field descriptor.
     * Object references come back as fully qualified names; primitives keep
     * their keyword form.
     */
    public static @NotNull String javaTypeOf(@NotNull String descriptor) {
        return switch (descriptor) {
            case "B" -> "byte";
            case "S" -> "short";
            case "C" -> "char";
            case "I" -> "int";
            case "Z" -> "boolean";
            case "J" -> "long";
            case "F" -> "float";
            case "D" -> "double";
            case "V" -> "void";
            default -> {
                if (descriptor.startsWith("L") && descriptor.endsWith(";")) {
                    yield descriptor.substring(1, descriptor.length() - 1).replace('/', '.');
                }
                if (descriptor.startsWith("[")) {
                    yield javaTypeOf(descriptor.substring(1)) + "[]";
                }
                throw new IllegalArgumentException("Unsupported descriptor: " + descriptor);
            }
        };
    }

    /**
     * Returns the simple class name for an object descriptor, or the
     * primitive keyword for a primitive descriptor.
     */
    public static @NotNull String simpleNameOf(@NotNull String descriptor) {
        String fq = javaTypeOf(descriptor);
        int dot = fq.lastIndexOf('.');
        return dot < 0 ? fq : fq.substring(dot + 1);
    }
}
