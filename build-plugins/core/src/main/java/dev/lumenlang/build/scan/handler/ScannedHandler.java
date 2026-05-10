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
 */
public record ScannedHandler(@NotNull String ownerInternalName, @NotNull Path classFile, @NotNull String methodName,
                             @NotNull String methodDescriptor, @NotNull HandlerKind kind,
                             @NotNull List<String> patterns, @NotNull List<InjectParam> injectParams,
                             @NotNull HandlerMeta meta) {
}
