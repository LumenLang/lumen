package dev.lumenlang.lumen.api.inject.index;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * One handler entry in the on-disk index emitted by the build plugin and
 * consumed by the runtime loader.
 *
 * @param kind        the handler kind ({@code "Statement"}, {@code "Expression"}, {@code "Condition"})
 * @param owner       JVM internal name of the declaring class
 * @param method      method name
 * @param descriptor  JVMS method descriptor
 * @param patterns    every {@code @Pattern} value, in source order
 * @param params      {@code @Inject} parameters in declaration order
 * @param meta        documentation metadata
 * @param methodBased true when the body should be emitted into a separate static method on the script class instead of inlined at the call site
 * @param wantsContext true when the original method takes a {@code HandlerContext} first argument; the runtime invokes the trimmed method at codegen so its compile section can mutate {@code ctx} before the runtime body emits
 */
public record IndexedHandler(@NotNull String kind, @NotNull String owner, @NotNull String method,
                             @NotNull String descriptor, @NotNull List<String> patterns,
                             @NotNull List<IndexedParam> params, @NotNull IndexedMeta meta, boolean methodBased,
                             boolean wantsContext) {
}
