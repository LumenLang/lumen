package dev.lumenlang.build.emit.slice;

import dev.lumenlang.build.source.ParsedHandlerSource;
import dev.lumenlang.build.source.phase.Phase;
import dev.lumenlang.build.source.phase.PhaseMarker;
import net.vansencool.vanta.parser.ast.declaration.MethodDeclaration;
import net.vansencool.vanta.parser.ast.statement.BlockStatement;
import net.vansencool.vanta.parser.ast.statement.Statement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Cuts the compile and runtime sections out of a handler method's source
 * text, using statement line numbers and phase markers as boundaries.
 *
 * <p>Lines holding phase markers themselves are dropped from the output;
 * blank-only lines around the boundaries are trimmed.
 */
public final class SourceSlicer {

    private SourceSlicer() {
    }

    public static @Nullable String compileSection(@NotNull ParsedHandlerSource parsed) {
        return sectionFor(parsed, Phase.COMPILE);
    }

    public static @NotNull String runtimeSection(@NotNull ParsedHandlerSource parsed) {
        String section = sectionFor(parsed, Phase.RUNTIME);
        return section == null ? "" : section;
    }

    private static @Nullable String sectionFor(@NotNull ParsedHandlerSource parsed, @NotNull Phase wanted) {
        MethodDeclaration method = parsed.method();
        BlockStatement body = method.body();
        if (body == null) return null;

        List<Statement> statements = body.statements();
        if (statements.isEmpty()) return wanted == Phase.RUNTIME ? "" : null;

        StringBuilder out = new StringBuilder();
        boolean any = false;
        Phase phase = Phase.RUNTIME;
        int markerIndex = 0;
        List<PhaseMarker> markers = parsed.markers();

        String[] lines = parsed.sourceText().split("\n", -1);
        int start = statements.get(0).line() - 1;
        int end = lastLineOf(statements);

        for (int i = start; i <= end && i < lines.length; i++) {
            int oneBased = i + 1;
            while (markerIndex < markers.size() && markers.get(markerIndex).line() == oneBased) {
                phase = markers.get(markerIndex).phase();
                markerIndex++;
            }
            if (phase != wanted) continue;
            if (isMarkerLine(markers, oneBased)) continue;
            out.append(lines[i]).append('\n');
            any = true;
        }

        if (!any) return wanted == Phase.RUNTIME ? "" : null;
        return out.toString().stripTrailing();
    }

    private static int lastLineOf(@NotNull List<Statement> statements) {
        int last = 0;
        for (Statement s : statements) if (s.line() > last) last = s.line();
        return last;
    }

    private static boolean isMarkerLine(@NotNull List<PhaseMarker> markers, int line) {
        for (PhaseMarker m : markers) if (m.line() == line) return true;
        return false;
    }
}
