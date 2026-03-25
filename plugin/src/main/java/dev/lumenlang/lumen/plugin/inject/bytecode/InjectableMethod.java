package dev.lumenlang.lumen.plugin.inject.bytecode;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Describes a bridge method that will be generated in the script class and later have
 * its body replaced with extracted bytecode.
 *
 * @param methodName the generated method name (e.g. "__injectable_0")
 * @param returnType the Java return type of the method (e.g. "void", "boolean", "Location")
 * @param parameterTypes the Java type names of each parameter, ordered by binding occurrence
 * @param parameterBindings the binding names corresponding to each parameter (e.g. "who", "other")
 * @param extractedBody the bytecode extracted from the addon's lambda or method
 */
public record InjectableMethod(@NotNull String methodName, @NotNull String returnType, @NotNull List<String> parameterTypes, @NotNull List<String> parameterBindings, @NotNull ExtractedBody extractedBody) {
}
