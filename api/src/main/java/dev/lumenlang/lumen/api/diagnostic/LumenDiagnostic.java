package dev.lumenlang.lumen.api.diagnostic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A rich diagnostic produced during Lumen script compilation or runtime.
 *
 * <p>Diagnostics capture source location, error context, and actionable suggestions.
 * They are formatted in a Rust-inspired style with underlines, contextual highlights,
 * notes, and help messages.
 *
 * <p>This is the standard error representation for the entire Lumen system.
 * All error paths (type mismatches, parse errors, null safety violations, pattern
 * failures, and addon errors) should produce diagnostics through this class.
 *
 * <h2>Error Code Convention</h2>
 * <p>Core Lumen diagnostics use plain numeric codes prefixed with {@code E}:
 * <ul>
 *   <li>{@code E1xx} for type errors (mismatch, null assignment, lossy conversion)</li>
 *   <li>{@code E2xx} for operator and arithmetic errors</li>
 *   <li>{@code E3xx} for null safety errors</li>
 *   <li>{@code E4xx} for collection errors</li>
 *   <li>{@code E5xx} for resolution errors (undefined variable, unknown type)</li>
 * </ul>
 *
 * <p>Addons should prefix their error codes with a short identifier derived from the
 * addon name, followed by {@code E} and a number. For example, an addon called
 * "MyAddon" would use codes like {@code MAE100}, {@code MAE201}, etc. This avoids
 * collisions with core codes and makes it clear which addon produced the diagnostic.
 *
 * <h2>Example Output</h2>
 * <pre>
 * error[E100]: Type mismatch in assignment
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

    private LumenDiagnostic(@NotNull Builder builder) {
        this.severity = builder.severity;
        this.code = builder.code;
        this.title = builder.title;
        this.line = builder.line;
        this.sourceText = builder.sourceText;
        this.underlineLabel = builder.underlineLabel;
        this.contextLines = List.copyOf(builder.contextLines);
        this.notes = List.copyOf(builder.notes);
        this.help = builder.help;
        if (builder.columnStart == -1 && builder.columnEnd == -1) {
            String stripped = sourceText.stripTrailing();
            this.columnStart = stripped.length() - stripped.stripLeading().length();
            this.columnEnd = stripped.length();
        } else {
            this.columnStart = builder.columnStart;
            this.columnEnd = builder.columnEnd;
        }
    }

    /**
     * A secondary source line shown for additional context in a multi-line diagnostic.
     *
     * <p>Context lines appear before the primary source line in the formatted output,
     * each with their own underline highlight and optional label. They are used to show
     * related code such as the declaration site or a previous assignment.
     *
     * @param line        the 1-based line number in the source file
     * @param source      the raw source text of the line
     * @param columnStart the 0-based start column of the highlight (inclusive)
     * @param columnEnd   the 0-based end column of the highlight (exclusive)
     * @param label       the label shown under the highlight, or {@code null} for no label
     */
    public record ContextLine(int line, @NotNull String source, int columnStart, int columnEnd, @Nullable String label) {
    }

    /**
     * Creates a new error diagnostic builder with the given code and title.
     *
     * @param code  the error code (e.g. {@code "E100"} for core, {@code "MAE100"} for addons)
     * @param title a short human-readable description of the error
     * @return a new builder
     */
    public static @NotNull Builder error(@NotNull String code, @NotNull String title) {
        return new Builder(Severity.ERROR, code, title);
    }

    /**
     * Creates a new warning diagnostic builder with the given code and title.
     *
     * @param code  the warning code (e.g. {@code "W100"} for core, {@code "MAW100"} for addons)
     * @param title a short human-readable description of the warning
     * @return a new builder
     */
    public static @NotNull Builder warning(@NotNull String code, @NotNull String title) {
        return new Builder(Severity.WARNING, code, title);
    }

    /**
     * Returns the severity level of this diagnostic.
     *
     * @return the severity
     */
    public @NotNull Severity severity() {
        return severity;
    }

    /**
     * Returns the error or warning code (e.g. {@code "E100"}).
     *
     * @return the diagnostic code
     */
    public @NotNull String code() {
        return code;
    }

    /**
     * Returns the short title describing the error.
     *
     * @return the diagnostic title
     */
    public @NotNull String title() {
        return title;
    }

    /**
     * Returns the 1-based line number in the source file where the error occurred.
     *
     * @return the source line number
     */
    public int line() {
        return line;
    }

    /**
     * Formats this diagnostic into a multi-line Rust-inspired error string.
     *
     * <p>The output includes the error header, source location, highlighted source lines
     * with underlines and labels, context lines, notes, and help suggestions.
     *
     * @return the formatted diagnostic string
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

    /**
     * The severity level of a diagnostic.
     */
    public enum Severity {
        /** A fatal error that prevents compilation. */
        ERROR,
        /** A non-fatal warning that does not prevent compilation. */
        WARNING
    }

    /**
     * Fluent builder for constructing {@link LumenDiagnostic} instances.
     *
     * <p>Use {@link LumenDiagnostic#error(String, String)} or
     * {@link LumenDiagnostic#warning(String, String)} to obtain a builder, then chain
     * calls to configure the diagnostic before calling {@link #build()}.
     *
     * <p>If {@link #highlight(int, int)} is not called, the entire source line content
     * (excluding leading whitespace) is highlighted by default.
     */
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

        /**
         * Sets the primary source location for this diagnostic.
         *
         * @param line       the 1-based line number where the error occurred
         * @param sourceText the raw source text of that line
         * @return this builder
         */
        public @NotNull Builder at(int line, @NotNull String sourceText) {
            this.line = line;
            this.sourceText = sourceText;
            return this;
        }

        /**
         * Sets the column range to highlight in the primary source line.
         *
         * <p>If this method is not called, the entire line content (excluding leading
         * whitespace) is highlighted by default.
         *
         * @param columnStart the 0-based start column (inclusive)
         * @param columnEnd   the 0-based end column (exclusive)
         * @return this builder
         */
        public @NotNull Builder highlight(int columnStart, int columnEnd) {
            this.columnStart = columnStart;
            this.columnEnd = columnEnd;
            return this;
        }

        /**
         * Sets the label displayed under the primary highlight.
         *
         * @param label a short description of the problem at this location
         * @return this builder
         */
        public @NotNull Builder label(@NotNull String label) {
            this.underlineLabel = label;
            return this;
        }

        /**
         * Adds a secondary context line to the diagnostic.
         *
         * <p>Context lines appear before the primary source line and are used to show
         * related code (e.g. the original declaration, a previous assignment, or a
         * conflicting definition).
         *
         * @param line        the 1-based line number of the context line
         * @param source      the raw source text
         * @param columnStart the 0-based start column of the highlight
         * @param columnEnd   the 0-based end column of the highlight
         * @param label       the label under the highlight, or {@code null}
         * @return this builder
         */
        public @NotNull Builder context(int line, @NotNull String source, int columnStart, int columnEnd, @Nullable String label) {
            this.contextLines.add(new ContextLine(line, source, columnStart, columnEnd, label));
            return this;
        }

        /**
         * Adds an informational note to the diagnostic.
         *
         * <p>Notes provide additional context such as where a variable was originally
         * declared or why a constraint exists.
         *
         * @param note the note text
         * @return this builder
         */
        public @NotNull Builder note(@NotNull String note) {
            this.notes.add(note);
            return this;
        }

        /**
         * Sets the help suggestion for the diagnostic.
         *
         * <p>The help text should describe a concrete action the user can take to fix
         * the problem.
         *
         * @param help the help text
         * @return this builder
         */
        public @NotNull Builder help(@NotNull String help) {
            this.help = help;
            return this;
        }

        /**
         * Builds the diagnostic from this builder's state.
         *
         * @return the constructed diagnostic
         */
        public @NotNull LumenDiagnostic build() {
            return new LumenDiagnostic(this);
        }
    }
}
