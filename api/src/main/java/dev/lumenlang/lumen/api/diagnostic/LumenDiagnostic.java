package dev.lumenlang.lumen.api.diagnostic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A rich diagnostic produced during Lumen script compilation or runtime.
 *
 * <p>Diagnostics capture source location, error context, and actionable suggestions.
 * They are formatted in a Rust-inspired style with underlines and contextual hints.
 *
 * <p>This is the standard error representation for the entire Lumen system.
 * All error paths (type mismatches, parse errors, pattern failures, addon errors)
 * should produce diagnostics through this class.
 *
 * <p>Example output:
 * <pre>
 * error[E001]: Type mismatch in assignment
 *   -> line 3: set x to "hello"
 *   |
 * 3 | set x to "hello"
 *   |          ~~~~~~~ expected 'int', found 'string'
 *   |
 *   = note: variable 'x' was declared as 'int' on line 1
 *   = help: once declared, a variable's type cannot change
 * </pre>
 */
public final class LumenDiagnostic {

    private final @NotNull Severity severity;
    private final @NotNull String code;
    private final @NotNull String title;
    private final int line;
    private final @NotNull String sourceText;
    private final int columnStart;
    private final int columnEnd;
    private final @Nullable String underlineLabel;
    private final @NotNull List<ContextLine> contextLines;
    private final @NotNull List<String> notes;
    private final @Nullable String help;

    private LumenDiagnostic(@NotNull Severity severity, @NotNull String code, @NotNull String title, int line, @NotNull String sourceText, int columnStart, int columnEnd, @Nullable String underlineLabel, @NotNull List<ContextLine> contextLines, @NotNull List<String> notes, @Nullable String help) {
        this.severity = severity;
        this.code = code;
        this.title = title;
        this.line = line;
        this.sourceText = sourceText;
        this.underlineLabel = underlineLabel;
        this.contextLines = contextLines;
        this.notes = notes;
        this.help = help;
        if (columnStart == -1 && columnEnd == -1) {
            String stripped = sourceText.stripTrailing();
            int leading = stripped.length() - stripped.stripLeading().length();
            this.columnStart = leading;
            this.columnEnd = stripped.length();
        } else {
            this.columnStart = columnStart;
            this.columnEnd = columnEnd;
        }
    }

    /**
     * A secondary source line shown for additional context in a multi-line diagnostic.
     *
     * @param line        the 1-based line number
     * @param source      the raw source text of the line
     * @param columnStart the 0-based start column of the highlight
     * @param columnEnd   the 0-based end column (exclusive) of the highlight
     * @param label       the label shown under the highlight
     */
    public record ContextLine(int line, @NotNull String source, int columnStart, int columnEnd, @Nullable String label) {
    }

    public static @NotNull Builder error(@NotNull String code, @NotNull String title) {
        return new Builder(Severity.ERROR, code, title);
    }

    public static @NotNull Builder warning(@NotNull String code, @NotNull String title) {
        return new Builder(Severity.WARNING, code, title);
    }

    public @NotNull Severity severity() {
        return severity;
    }

    public @NotNull String code() {
        return code;
    }

    public @NotNull String title() {
        return title;
    }

    public int line() {
        return line;
    }

    /**
     * Formats this diagnostic into a multi-line Rust-inspired error string.
     *
     * @return the formatted diagnostic
     */
    public @NotNull String format() {
        StringBuilder sb = new StringBuilder();
        String prefix = severity == Severity.ERROR ? "error" : "warning";
        sb.append(prefix).append('[').append(code).append("]: ").append(title).append('\n');
        String trimmed = sourceText.stripTrailing();
        int maxLine = line;
        for (ContextLine cl : contextLines) {
            if (cl.line > maxLine) maxLine = cl.line;
        }
        String maxLineNum = String.valueOf(maxLine);
        String gutter = " ".repeat(maxLineNum.length()) + " |";
        sb.append("  -> line ").append(line).append(": ").append(trimmed.stripLeading()).append('\n');
        sb.append(gutter).append('\n');
        for (ContextLine cl : contextLines) {
            String ctxTrimmed = cl.source.stripTrailing();
            String ctxLineNum = String.valueOf(cl.line);
            String padded = " ".repeat(maxLineNum.length() - ctxLineNum.length()) + ctxLineNum;
            sb.append(padded).append(" | ").append(ctxTrimmed).append('\n');
            int ctxStart = Math.max(0, Math.min(cl.columnStart, ctxTrimmed.length()));
            int ctxEnd = Math.max(ctxStart + 1, Math.min(cl.columnEnd, ctxTrimmed.length()));
            sb.append(gutter).append(" ".repeat(ctxStart + 1)).append("~".repeat(Math.max(1, ctxEnd - ctxStart)));
            if (cl.label != null) sb.append(' ').append(cl.label);
            sb.append('\n');
            sb.append(gutter).append('\n');
            if (cl.line + 1 < line) {
                sb.append(" ".repeat(maxLineNum.length())).append(" ...").append('\n');
                sb.append(gutter).append('\n');
            }
        }
        String lineNum = String.valueOf(line);
        String padded = " ".repeat(maxLineNum.length() - lineNum.length()) + lineNum;
        sb.append(padded).append(" | ").append(trimmed).append('\n');
        int start = Math.max(0, Math.min(columnStart, trimmed.length()));
        int end = Math.max(start + 1, Math.min(columnEnd, trimmed.length()));
        sb.append(gutter).append(" ".repeat(start + 1)).append("~".repeat(Math.max(1, end - start)));
        if (underlineLabel != null) {
            sb.append(' ').append(underlineLabel);
        }
        sb.append('\n');
        sb.append(gutter).append('\n');
        for (String note : notes) {
            sb.append("  = note: ").append(note).append('\n');
        }
        if (help != null) {
            sb.append("  = help: ").append(help).append('\n');
        }
        return sb.toString();
    }

    public enum Severity {
        ERROR,
        WARNING
    }

    public static final class Builder {
        private final @NotNull Severity severity;
        private final @NotNull String code;
        private final @NotNull String title;
        private int line;
        private @NotNull String sourceText = "";
        private int columnStart = -1;
        private int columnEnd = -1;
        private @Nullable String underlineLabel;
        private final @NotNull ArrayList<ContextLine> contextLines = new ArrayList<>();
        private final @NotNull ArrayList<String> notes = new ArrayList<>();
        private @Nullable String help;

        private Builder(@NotNull Severity severity, @NotNull String code, @NotNull String title) {
            this.severity = severity;
            this.code = code;
            this.title = title;
        }

        public @NotNull Builder at(int line, @NotNull String sourceText) {
            this.line = line;
            this.sourceText = sourceText;
            return this;
        }

        public @NotNull Builder highlight(int columnStart, int columnEnd) {
            this.columnStart = columnStart;
            this.columnEnd = columnEnd;
            return this;
        }

        public @NotNull Builder label(@NotNull String label) {
            this.underlineLabel = label;
            return this;
        }

        public @NotNull Builder context(int line, @NotNull String source, int columnStart, int columnEnd, @Nullable String label) {
            this.contextLines.add(new ContextLine(line, source, columnStart, columnEnd, label));
            return this;
        }

        public @NotNull Builder note(@NotNull String note) {
            this.notes.add(note);
            return this;
        }

        public @NotNull Builder help(@NotNull String help) {
            this.help = help;
            return this;
        }

        public @NotNull LumenDiagnostic build() {
            return new LumenDiagnostic(severity, code, title, line, sourceText, columnStart, columnEnd, underlineLabel, List.copyOf(contextLines), List.copyOf(notes), help);
        }
    }
}
