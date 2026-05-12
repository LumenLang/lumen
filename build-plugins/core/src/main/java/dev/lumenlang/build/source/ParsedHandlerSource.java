package dev.lumenlang.build.source;

import dev.lumenlang.build.source.phase.PhaseMarker;
import net.vansencool.vanta.parser.ast.declaration.CompilationUnit;
import net.vansencool.vanta.parser.ast.declaration.MethodDeclaration;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

/**
 * Result of parsing one handler's source file: the full compilation unit (so
 * downstream checks have access to imports, sibling members, etc.), the
 * matched method declaration, the file path, and the captured phase markers.
 *
 * @param sourceFile      {@code .java} file the AST came from
 * @param sourceText      the file's full text, for slicing and the sidecar
 * @param compilationUnit the parsed AST root
 * @param method          the {@link MethodDeclaration} that owns the handler body
 * @param markers         every phase marker captured before parsing, in source order
 */
public record ParsedHandlerSource(@NotNull Path sourceFile, @NotNull String sourceText,
                                  @NotNull CompilationUnit compilationUnit, @NotNull MethodDeclaration method,
                                  @NotNull List<PhaseMarker> markers) {
}
