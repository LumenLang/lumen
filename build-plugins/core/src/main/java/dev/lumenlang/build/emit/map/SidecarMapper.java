package dev.lumenlang.build.emit.map;

import dev.lumenlang.build.emit.slice.SourceSlicer;
import dev.lumenlang.build.emit.slice.SourceStatement;
import dev.lumenlang.build.emit.slice.StatementSlicer;
import dev.lumenlang.build.scan.handler.ScannedHandler;
import dev.lumenlang.build.source.ParsedHandlerSource;
import dev.lumenlang.lumen.api.inject.index.SidecarEntry;
import net.vansencool.vanta.parser.ast.declaration.ImportDeclaration;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a {@link SidecarEntry} for a scanned handler. Pre-extracts the
 * runtime body's structured shape (statement texts, return expression,
 * always-throws flag) so the runtime never parses Java.
 */
public final class SidecarMapper {

    private static final String LUMEN_ANNOTATION_PREFIX = "dev.lumenlang.lumen.api.pattern.annotation.";

    private SidecarMapper() {
    }

    public static @NotNull SidecarEntry map(@NotNull ScannedHandler scanned, @NotNull ParsedHandlerSource parsed) {
        List<SourceStatement> runtimeStatements = StatementSlicer.sliceRuntime(parsed);
        List<String> bodyLines = new ArrayList<>(runtimeStatements.size());
        for (SourceStatement s : runtimeStatements) bodyLines.add(s.text());
        return new SidecarEntry(
                scanned.ownerInternalName(),
                scanned.methodName(),
                scanned.methodDescriptor(),
                importsOf(parsed),
                SourceSlicer.compileSection(parsed),
                List.copyOf(bodyLines),
                StatementSlicer.returnExpression(runtimeStatements),
                StatementSlicer.alwaysThrows(runtimeStatements));
                // scanned.wantsContext()
    }

    private static @NotNull List<String> importsOf(@NotNull ParsedHandlerSource parsed) {
        List<String> result = new ArrayList<>();
        for (ImportDeclaration imp : parsed.compilationUnit().imports()) {
            if (imp.isStatic() || imp.isWildcard()) continue;
            if (imp.name().startsWith(LUMEN_ANNOTATION_PREFIX)) continue;
            result.add(imp.name());
        }
        return List.copyOf(result);
    }
}
