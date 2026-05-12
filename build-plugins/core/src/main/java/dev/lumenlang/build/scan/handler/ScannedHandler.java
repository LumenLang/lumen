package dev.lumenlang.build.scan.handler;

import dev.lumenlang.build.scan.ClassScanner;
import dev.lumenlang.build.scan.param.InjectParam;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

/**
 * Bytecode-level summary of one annotated handler method, produced by
 * {@link ClassScanner}. Source-level checks consume this as input.
 *
 * @param ownerInternalName internal class name (slashes), e.g. {@code com/example/foo/MyHandlers}
 * @param classFile         absolute path of the {@code .class} file the method lives in
 * @param methodName        the method's identifier
 * @param methodDescriptor  JVMS method descriptor, e.g. {@code (Ljava/lang/String;I)V}
 * @param kind              which kind annotation marked it
 * @param patterns          pattern strings from {@code @Pattern} (one or more)
 * @param injectParams      parameters carrying {@code @Inject}, in declaration order
 * @param meta              optional documentation metadata
 * @param methodBased       true when {@code @MethodBased} is present on the handler (statements only)
 * @param wantsContext      true when the method's first parameter is a {@code HandlerContext}; the build plugin
 *                          strips the runtime-section bytecode so invoking the method runs only the compile section
 */
public record ScannedHandler(@NotNull String ownerInternalName, @NotNull Path classFile, @NotNull String methodName,
                             @NotNull String methodDescriptor, @NotNull HandlerKind kind,
                             @NotNull List<String> patterns, @NotNull List<InjectParam> injectParams,
                             @NotNull HandlerMeta meta, boolean methodBased, boolean wantsContext) {
}
