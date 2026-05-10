package dev.lumenlang.lumen.pipeline.inject.sidecar;

import org.jetbrains.annotations.NotNull;

/**
 * Converts JVMS field descriptors into the Java type names used at codegen
 * time when emitting bridge methods or import lists.
 */
public final class Descriptors {

    private Descriptors() {
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
