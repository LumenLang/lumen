package dev.lumenlang.lumen.api.diagnostic;

import dev.lumenlang.console.border.BorderStyle;
import dev.lumenlang.console.element.Element;
import dev.lumenlang.console.element.Renderer;
import dev.lumenlang.console.element.impl.widget.Card;
import dev.lumenlang.console.element.layout.Column;
import dev.lumenlang.console.style.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static dev.lumenlang.console.UIUtils.col;
import static dev.lumenlang.console.UIUtils.hspace;
import static dev.lumenlang.console.UIUtils.row;
import static dev.lumenlang.console.UIUtils.text;
import static dev.lumenlang.console.UIUtils.vspace;

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
 */
@SuppressWarnings("unused")
public final class LumenDiagnostic {

    private static final int LONG_LINE_THRESHOLD = 80;
    private static boolean verboseDiagnostics = false;
    private static boolean styled = false;

    private final @NotNull Severity severity;
    private final @NotNull String title;
    private final int line;
    private final @NotNull String sourceText;
    private final int columnStart;
    private final int columnEnd;
    private final @Nullable String underlineLabel;
    private final @NotNull List<SubHighlight> subHighlights;
    private final @NotNull List<ContextLine> contextLines;
    private final @NotNull List<String> notes;
    private final @NotNull List<String> helpLines;

    private LumenDiagnostic(@NotNull Builder builder) {
        this.severity = builder.severity;
        this.title = builder.title;
        this.line = builder.line;
        this.sourceText = builder.sourceText;
        this.underlineLabel = builder.underlineLabel;
        this.subHighlights = List.copyOf(builder.subHighlights);
        this.contextLines = List.copyOf(builder.contextLines);
        this.notes = List.copyOf(builder.notes);
        this.helpLines = List.copyOf(builder.helpLines);
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
     * Creates a new error diagnostic builder with the given title.
     *
     * @param title a short human-readable description of the error
     * @return a new builder
     */
    public static @NotNull Builder error(@NotNull String title) {
        return new Builder(Severity.ERROR, title);
    }

    /**
     * Creates a new warning diagnostic builder with the given title.
     *
     * @param title a short human-readable description of the warning
     * @return a new builder
     */
    public static @NotNull Builder warning(@NotNull String title) {
        return new Builder(Severity.WARNING, title);
    }

    /**
     * Configures verbose diagnostic mode globally.
     *
     * <p>When enabled, diagnostics highlight the entire source line and render all
     * individual labels as notes instead of inline underlines.
     *
     * @param verbose whether verbose diagnostics are enabled
     */
    public static void configureVerbose(boolean verbose) {
        verboseDiagnostics = verbose;
    }

    /**
     * Enables ANSI-styled diagnostic rendering globally. When on, {@link #format()} returns text
     * with truecolor escapes, glyphs, and emphasis suitable for a terminal that supports them.
     */
    public static void configureStyled(boolean enabled) {
        styled = enabled;
    }

    /**
     * Returns whether styled rendering is currently enabled.
     */
    public static boolean styled() {
        return styled;
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
     * Returns the short title describing the error.
     *
     * @return the diagnostic title
     */
    public @NotNull String title() {
        return title;
    }

    /**
     * Returns the 1-based line number in the source file where the error occurred.
     */
    public int line() {
        return line;
    }

    /**
     * Returns the raw source text of the offending line.
     */
    public @NotNull String sourceText() {
        return sourceText;
    }

    /**
     * Returns the 0-based starting column of the primary highlight on the source line.
     */
    public int columnStart() {
        return columnStart;
    }

    /**
     * Returns the 0-based ending column (exclusive) of the primary highlight on the source line.
     */
    public int columnEnd() {
        return columnEnd;
    }

    /**
     * Returns the inline label rendered next to the primary underline, or {@code null}.
     */
    public @Nullable String underlineLabel() {
        return underlineLabel;
    }

    /**
     * Returns all secondary highlights on the offending line, in declaration order.
     */
    public @NotNull List<SubHighlight> subHighlights() {
        return subHighlights;
    }

    /**
     * Returns the surrounding context lines that precede the primary line, in declaration order.
     */
    public @NotNull List<ContextLine> contextLines() {
        return contextLines;
    }

    /**
     * Returns the additional notes attached to this diagnostic, in declaration order.
     */
    public @NotNull List<String> notes() {
        return notes;
    }

    /**
     * Returns the help suggestions attached to this diagnostic, in declaration order.
     */
    public @NotNull List<String> helpLines() {
        return helpLines;
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
        if (styled) return formatFancy();
        return formatPlain();
    }

    /**
     * Renders a group of diagnostics as a single block. When styled mode is on, returns one
     * combined fancy box; otherwise returns plain output joined with blank lines.
     */
    public static @NotNull String formatGroup(@NotNull List<LumenDiagnostic> diagnostics) {
        if (diagnostics.isEmpty()) return "";
        if (styled) return formatGroupFancy(diagnostics);
        return formatGroupPlain(diagnostics);
    }

    private static @NotNull String formatGroupPlain(@NotNull List<LumenDiagnostic> diagnostics) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < diagnostics.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(diagnostics.get(i).formatPlain());
        }
        return sb.toString();
    }

    private static @NotNull String formatGroupFancy(@NotNull List<LumenDiagnostic> diagnostics) {
        int errorCount = 0;
        int warnCount = 0;
        for (LumenDiagnostic d : diagnostics) {
            if (d.severity == Severity.ERROR) errorCount++;
            else warnCount++;
        }
        Color groupAccent = errorCount > 0 ? Color.ALARM_RED : Color.SOFT_PEACH;

        Element groupHeader;
        if (errorCount > 0 && warnCount > 0) {
            groupHeader = row(
                    text(" " + errorCount + " ERROR" + (errorCount == 1 ? "" : "S") + " ").bg(Color.ALARM_RED).fg(Color.INK).bold(),
                    hspace(2),
                    text(" " + warnCount + " WARN" + (warnCount == 1 ? "" : "S") + " ").bg(Color.SOFT_PEACH).fg(Color.INK).bold()
            );
        } else if (errorCount > 0) {
            groupHeader = text(" " + errorCount + " ERROR" + (errorCount == 1 ? "" : "S") + " ").bg(Color.ALARM_RED).fg(Color.INK).bold();
        } else {
            groupHeader = text(" " + warnCount + " WARN" + (warnCount == 1 ? "" : "S") + " ").bg(Color.SOFT_PEACH).fg(Color.INK).bold();
        }

        List<Element> blocks = new ArrayList<>();
        blocks.add(groupHeader);
        blocks.add(vspace(1));
        for (int i = 0; i < diagnostics.size(); i++) {
            if (i > 0) {
                blocks.add(vspace(1));
                blocks.add(text("─".repeat(76)).fg(Color.SLATE).dim());
                blocks.add(vspace(1));
            }
            blocks.add(diagnostics.get(i).fancyBody());
        }
        Element body = Column.of(blocks.toArray(new Element[0]));
        Element card = Card.of(body)
                .border(BorderStyle.ROUNDED)
                .borderColor(groupAccent)
                .padding(1, 2);
        return Renderer.render(card, 80, 1);
    }

    private @NotNull String formatPlain() {
        StringBuilder sb = new StringBuilder();
        String prefix = severity == Severity.ERROR ? "error" : "warning";
        sb.append(prefix).append(": ").append(title).append('\n');
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
        boolean verbose = verboseDiagnostics;
        boolean longLine = !verbose && !subHighlights.isEmpty() && trimmed.length() > LONG_LINE_THRESHOLD;
        boolean subHighlightsAsNotes = verbose || longLine;
        if (verbose) {
            String stripped = trimmed.stripLeading();
            int fullStart = trimmed.length() - stripped.length();
            sb.append(gutter).append(" ".repeat(fullStart + 1)).append("~".repeat(stripped.length()));
            sb.append('\n');
        } else {
            int start = Math.max(0, Math.min(columnStart, trimmed.length()));
            int end = Math.max(start + 1, Math.min(columnEnd, trimmed.length()));
            sb.append(gutter).append(" ".repeat(start + 1)).append("~".repeat(Math.max(1, end - start)));
            if (underlineLabel != null) sb.append(' ').append(underlineLabel);
            sb.append('\n');
        }
        if (!subHighlightsAsNotes) {
            for (SubHighlight sh : subHighlights) {
                int shStart = Math.max(0, Math.min(sh.columnStart, trimmed.length()));
                int shEnd = Math.max(shStart + 1, Math.min(sh.columnEnd, trimmed.length()));
                sb.append(gutter).append(" ".repeat(shStart + 1)).append("~".repeat(Math.max(1, shEnd - shStart)));
                if (sh.label != null) sb.append(' ').append(sh.label);
                sb.append('\n');
            }
        }
        sb.append(gutter).append('\n');
        if (verbose && underlineLabel != null) {
            sb.append("  = note: ").append(underlineLabel).append('\n');
        }
        if (subHighlightsAsNotes) {
            for (SubHighlight sh : subHighlights) {
                if (sh.label == null) continue;
                sb.append("  = note: ").append(sh.label).append('\n');
            }
        }
        for (String note : notes) {
            sb.append("  = note: ").append(note).append('\n');
        }
        for (String h : helpLines) {
            sb.append("  = help: ").append(h).append('\n');
        }
        return sb.toString();
    }

    private @NotNull String formatFancy() {
        boolean isError = severity == Severity.ERROR;
        Color accent = isError ? Color.ALARM_RED : Color.SOFT_PEACH;
        Element card = Card.of(fancyBody())
                .border(BorderStyle.ROUNDED)
                .borderColor(accent)
                .padding(1, 2);
        return Renderer.render(card, 80, 1);
    }

    private @NotNull Element fancyBody() {
        boolean isError = severity == Severity.ERROR;
        Color accent = isError ? Color.ALARM_RED : Color.SOFT_PEACH;
        String levelLabel = isError ? " ERROR " : " WARN  ";

        Element header = row(
                text(levelLabel).bg(accent).fg(Color.INK).bold(),
                hspace(2),
                text(title).fg(Color.BONE).bold()
        );

        String trimmed = sourceText.stripTrailing();
        int maxLineNum = line;
        for (ContextLine cl : contextLines) if (cl.line > maxLineNum) maxLineNum = cl.line;
        int gutterWidth = String.valueOf(maxLineNum).length();

        List<Element> rows = new ArrayList<>();
        rows.add(row(
                text("at ").fg(Color.SLATE),
                text("line " + line).fg(Color.OCEAN).bold()
        ));
        rows.add(vspace(1));

        for (ContextLine cl : contextLines) {
            String ctxTrimmed = cl.source.stripTrailing();
            rows.add(codeRow(cl.line, ctxTrimmed, gutterWidth));
            int ctxStart = Math.max(0, Math.min(cl.columnStart, ctxTrimmed.length()));
            int ctxEnd = Math.max(ctxStart + 1, Math.min(cl.columnEnd, ctxTrimmed.length()));
            rows.add(highlightRow(ctxStart, ctxEnd - ctxStart, cl.label, accent, gutterWidth));
            if (cl.line + 1 < line) rows.add(row(
                    text(padLine(gutterWidth)).fg(Color.SLATE).dim(),
                    text(" ⋮").fg(Color.SLATE).dim()
            ));
        }

        rows.add(codeRow(line, trimmed, gutterWidth));

        boolean verbose = verboseDiagnostics;
        boolean longLine = !verbose && !subHighlights.isEmpty() && trimmed.length() > LONG_LINE_THRESHOLD;
        boolean subHighlightsAsNotes = verbose || longLine;
        if (verbose) {
            String stripped = trimmed.stripLeading();
            int fullStart = trimmed.length() - stripped.length();
            rows.add(highlightRow(fullStart, stripped.length(), null, accent, gutterWidth));
        } else {
            int start = Math.max(0, Math.min(columnStart, trimmed.length()));
            int end = Math.max(start + 1, Math.min(columnEnd, trimmed.length()));
            rows.add(highlightRow(start, end - start, underlineLabel, accent, gutterWidth));
        }
        if (!subHighlightsAsNotes) {
            for (SubHighlight sh : subHighlights) {
                int shStart = Math.max(0, Math.min(sh.columnStart, trimmed.length()));
                int shEnd = Math.max(shStart + 1, Math.min(sh.columnEnd, trimmed.length()));
                rows.add(highlightRow(shStart, shEnd - shStart, sh.label, accent, gutterWidth));
            }
        }

        if (verbose && underlineLabel != null) rows.add(noteRow(underlineLabel));
        if (subHighlightsAsNotes) {
            for (SubHighlight sh : subHighlights) {
                if (sh.label != null) rows.add(noteRow(sh.label));
            }
        }
        for (String note : notes) rows.add(noteRow(note));
        for (String h : helpLines) rows.add(helpRow(h));

        return col(header, vspace(1), Column.of(rows.toArray(new Element[0])));
    }

    private static @NotNull Element codeRow(int lineNum, @NotNull String src, int gutterWidth) {
        String paddedNum = String.format("%" + gutterWidth + "d", lineNum);
        return row(
                text(paddedNum).fg(Color.OCEAN).bold(),
                text(" │ ").fg(Color.SLATE),
                text(src).fg(Color.BONE)
        );
    }

    private static @NotNull Element highlightRow(int column, int length, @Nullable String label, @NotNull Color accent, int gutterWidth) {
        if (label == null) {
            return row(
                    text(padLine(gutterWidth)).fg(Color.SLATE),
                    text(" │ ").fg(Color.SLATE),
                    text(" ".repeat(column)).fg(Color.SLATE),
                    text("▔".repeat(Math.max(1, length))).fg(accent).bold()
            );
        }
        return row(
                text(padLine(gutterWidth)).fg(Color.SLATE),
                text(" │ ").fg(Color.SLATE),
                text(" ".repeat(column)).fg(Color.SLATE),
                text("▔".repeat(Math.max(1, length))).fg(accent).bold(),
                text(" ").dim(),
                text(label).fg(accent).italic()
        );
    }

    private static @NotNull Element noteRow(@NotNull String text2) {
        return row(
                text("◆ note  ").fg(Color.OCEAN).bold(),
                text(text2).fg(Color.BONE)
        );
    }

    private static @NotNull Element helpRow(@NotNull String text2) {
        return row(
                text("✦ help  ").fg(Color.MINT).bold(),
                text(text2).fg(Color.BONE)
        );
    }

    private static @NotNull String padLine(int width) {
        return " ".repeat(width);
    }

    /**
     * The severity level of a diagnostic.
     */
    public enum Severity {
        /**
         * A fatal error that prevents compilation.
         */
        ERROR,
        /**
         * A non-fatal warning that does not prevent compilation.
         */
        WARNING
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
    public record ContextLine(int line, @NotNull String source, int columnStart, int columnEnd,
                              @Nullable String label) {
    }

    /**
     * An additional highlight on the primary source line, rendered below the main underline.
     *
     * @param columnStart the 0-based start column of the highlight (inclusive)
     * @param columnEnd   the 0-based end column of the highlight (exclusive)
     * @param label       the label shown under the highlight, or {@code null} for no label
     */
    public record SubHighlight(int columnStart, int columnEnd, @Nullable String label) {
    }

    /**
     * Fluent builder for constructing {@link LumenDiagnostic} instances.
     *
     * <p>Use {@link LumenDiagnostic#error(String)} or
     * {@link LumenDiagnostic#warning(String)} to obtain a builder, then chain
     * calls to configure the diagnostic before calling {@link #build()}.
     *
     * <p>If {@link #highlight(int, int)} is not called, the entire source line content
     * (excluding leading whitespace) is highlighted by default.
     */
    public static final class Builder {
        private final @NotNull Severity severity;
        private final @NotNull String title;
        private final @NotNull ArrayList<SubHighlight> subHighlights = new ArrayList<>();
        private final @NotNull ArrayList<ContextLine> contextLines = new ArrayList<>();
        private final @NotNull ArrayList<String> notes = new ArrayList<>();
        private int line;
        private @NotNull String sourceText = "";
        private int columnStart = -1;
        private int columnEnd = -1;
        private @Nullable String underlineLabel;
        private final @NotNull ArrayList<String> helpLines = new ArrayList<>();

        private Builder(@NotNull Severity severity, @NotNull String title) {
            this.severity = severity;
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
         * Adds a secondary highlight on the primary source line.
         *
         * @param columnStart the 0-based start column (inclusive)
         * @param columnEnd   the 0-based end column (exclusive)
         * @param label       the label under the highlight, or {@code null}
         * @return this builder
         */
        public @NotNull Builder subHighlight(int columnStart, int columnEnd, @Nullable String label) {
            this.subHighlights.add(new SubHighlight(columnStart, columnEnd, label));
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
         * Adds a help suggestion for the diagnostic.
         *
         * <p>The help text should describe a concrete action the user can take to fix the problem.
         *
         * @param help the help text
         * @return this builder
         */
        public @NotNull Builder help(@NotNull String help) {
            this.helpLines.add(help);
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
