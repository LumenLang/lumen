package dev.lumenlang.build.emit.slice;

import dev.lumenlang.build.source.ParsedHandlerSource;
import dev.lumenlang.build.source.phase.Phase;
import dev.lumenlang.build.source.phase.PhaseMarker;
import net.vansencool.vanta.parser.ast.declaration.MethodDeclaration;
import net.vansencool.vanta.parser.ast.expression.Expression;
import net.vansencool.vanta.parser.ast.statement.BlockStatement;
import net.vansencool.vanta.parser.ast.statement.ReturnStatement;
import net.vansencool.vanta.parser.ast.statement.Statement;
import net.vansencool.vanta.parser.ast.statement.ThrowStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Slices a handler's runtime section into individual top-level statement
 * texts using AST line numbers as boundaries. Source text comes from the
 * original {@code .java} file; the AST decides where statements begin and
 * end.
 */
public final class StatementSlicer {

    private StatementSlicer() {
    }

    public static @NotNull List<SourceStatement> sliceRuntime(@NotNull ParsedHandlerSource parsed) {
        MethodDeclaration method = parsed.method();
        BlockStatement body = method.body();
        if (body == null) return List.of();

        List<Statement> runtimeStatements = filterRuntime(body.statements(), parsed.markers());
        if (runtimeStatements.isEmpty()) return List.of();

        String[] lines = parsed.sourceText().split("\\R", -1);
        List<SourceStatement> out = new ArrayList<>(runtimeStatements.size());
        for (int i = 0; i < runtimeStatements.size(); i++) {
            Statement stmt = runtimeStatements.get(i);
            int start = stmt.line() - 1;
            int end = i + 1 < runtimeStatements.size() ? runtimeStatements.get(i + 1).line() - 1 : lastLine(body);
            String text = sliceLines(lines, start, end);
            out.add(new SourceStatement(stmt, text));
        }
        return out;
    }

    public static @Nullable String returnExpression(@NotNull List<SourceStatement> statements) {
        if (statements.size() != 1) return null;
        SourceStatement only = statements.get(0);
        if (!(only.node() instanceof ReturnStatement ret)) return null;
        Expression value = ret.value();
        if (value == null) return null;
        String text = only.text().strip();
        if (!text.startsWith("return") || !text.endsWith(";")) return null;
        String body = text.substring("return".length(), text.length() - 1).strip();
        return body.isEmpty() ? null : body;
    }

    public static boolean alwaysThrows(@NotNull List<SourceStatement> statements) {
        return statements.size() == 1 && statements.get(0).node() instanceof ThrowStatement;
    }

    private static @NotNull List<Statement> filterRuntime(@NotNull List<Statement> statements, @NotNull List<PhaseMarker> markers) {
        List<Statement> out = new ArrayList<>();
        for (Statement s : statements) {
            if (phaseAt(s.line(), markers) == Phase.RUNTIME) out.add(s);
        }
        return out;
    }

    private static @NotNull Phase phaseAt(int line, @NotNull List<PhaseMarker> markers) {
        Phase current = Phase.RUNTIME;
        for (PhaseMarker m : markers) {
            if (m.line() > line) break;
            current = m.phase();
        }
        return current;
    }

    /**
     * Exclusive 0-indexed end-of-body line. Statements on source line N occupy
     * {@code lines[N-1]}; the line AFTER the last statement is {@code lines[N]},
     * which is what we return so the slicer stops at it.
     */
    private static int lastLine(@NotNull BlockStatement body) {
        int last = body.line();
        for (Statement s : body.statements()) if (s.line() > last) last = s.line();
        return last;
    }

    private static @NotNull String sliceLines(@NotNull String[] lines, int startInclusive, int endExclusive) {
        StringBuilder sb = new StringBuilder();
        for (int i = startInclusive; i < endExclusive && i < lines.length; i++) {
            if (!sb.isEmpty()) sb.append('\n');
            sb.append(lines[i]);
        }
        return sb.toString().strip();
    }
}
