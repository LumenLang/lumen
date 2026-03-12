package dev.lumenlang.lumen.pipeline.java.formatter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A lightweight Java code formatter providing multiple levels of source transformation.
 *
 * <ul>
 *   <li>{@link #format} - formatted, with identical branches merged</li>
 *   <li>{@link #formatReadable} - same as {@link #format} but also strips all cast expressions,
 *       producing output that may not compile but is easier to read quickly</li>
 * </ul>
 */
@SuppressWarnings("unused")
public final class MiniJavaFormatter {

    private static final Pattern CAST_PATTERN =
            Pattern.compile("\\((?:int|long|double|float|short|byte|char|boolean|[A-Z]\\w*(?:\\.\\w+)*(?:<[^)]*>)?(?:\\[])*)\\)");

    /**
     * Formats the source and merges consecutive identical if-blocks. The result is still valid Java.
     *
     * @param source raw Java source
     * @return formatted and cleaned Java source
     */
    public static @NotNull String format(@NotNull String source) {
        String indented = applyIndentation(source,
                """
                        // Formatted by MiniJavaFormatter (branches merged).
                        """);
        return mergeIdenticalBranches(indented);
    }

    /**
     * Formats the source, merges identical branches, and additionally removes all cast expressions
     * such as {@code (long)}, {@code (Object)}, {@code (Player)}, etc.
     * The resulting source is intended for human reading only and may not compile.
     *
     * @param source raw Java source
     * @return human-readable source with casts removed
     */
    public static @NotNull String formatReadable(@NotNull String source) {
        String indented = applyIndentation(source,
                """
                        // Formatted by MiniJavaFormatter (readable: branches merged, casts removed, no String.valueOf or .toString).
                        // This file is for reading only - it may not compile.
                        """);
        String merged = mergeIdenticalBranches(indented);
        return stripCasts(stripToString(stripStringValueOf(merged)));
    }

    /**
     * Applies indentation and brace normalization to the source, prepending the provided header comment.
     * This is the shared formatting core used by all public format variants.
     *
     * @param source raw Java source
     * @param header comment lines to prepend (already newline-terminated)
     * @return indentation-normalized source with the header prepended
     */
    private static @NotNull String applyIndentation(@NotNull String source, @NotNull String header) {
        String[] lines = normalizeLines(source.split("\\R"));
        StringBuilder out = new StringBuilder(source.length() * 2);
        out.append(header);
        int indent = 0;
        boolean lastLineBlank = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (line.isEmpty()) {
                if (!lastLineBlank && !out.isEmpty()) {
                    out.append('\n');
                    lastLineBlank = true;
                }
                continue;
            }

            lastLineBlank = false;

            if (line.startsWith("public class") && !out.isEmpty()
                    && !endsWithBlankLine(out)) {
                out.append('\n');
            }

            if (isOnlyClosingBraces(line)) {
                for (int j = 0; j < line.length(); j++) {
                    indent = Math.max(0, indent - 1);
                    out.append(" ".repeat(indent * 4));
                    out.append('}');
                    out.append('\n');
                }
                if (indent == 1 && i + 1 < lines.length) {
                    String next = lines[i + 1].trim();
                    if (!next.equals("}")) {
                        out.append('\n');
                    }
                }
                continue;
            }

            String expanded = expandSingleLineBlock(line);
            if (expanded != null) {
                String[] parts = expanded.split("\\R");
                for (String s : parts) {
                    String part = s.trim();
                    if (part.isEmpty()) continue;

                    int pOpens = countCodeBraces(part, '{');
                    int pCloses = countCodeBraces(part, '}');

                    applyBraceIndent(out, part, pOpens, pCloses, indent);
                    indent = nextIndent(indent, pOpens, pCloses, part);
                }

                if (indent == 1 && i + 1 < lines.length) {
                    String next = lines[i + 1].trim();
                    if (!next.equals("}")) {
                        out.append('\n');
                    }
                }
                continue;
            }

            int opens = countCodeBraces(line, '{');
            int closes = countCodeBraces(line, '}');

            indent = preWriteIndent(indent, opens, closes, line);

            out.append(" ".repeat(indent * 4));
            out.append(line);
            out.append('\n');

            indent = postWriteIndent(indent, opens, closes, line);

            if (opens > 0 && line.startsWith("public class")) {
                if (!endsWithBlankLine(out)) {
                    out.append('\n');
                }
                lastLineBlank = true;
            }

            if (line.equals("}") && indent == 1 && i + 1 < lines.length) {
                String next = lines[i + 1].trim();
                if (!next.equals("}")) {
                    out.append('\n');
                }
            }
        }

        return out.toString().trim();
    }

    /**
     * Returns the indent level to use when writing {@code line}, accounting for closing braces.
     * Balanced braces on lines that do not start with {@code '}'} (e.g. annotation array values)
     * do not cause a pre-write dedent.
     *
     * @param indent current indent level
     * @param opens  open-brace count on the line
     * @param closes close-brace count on the line
     * @param line   trimmed source line
     * @return indent level to use when writing this line
     */
    private static int preWriteIndent(int indent, int opens, int closes, @NotNull String line) {
        boolean startsWithClose = line.startsWith("}");
        if (closes > 0 && (closes != opens || startsWithClose)) {
            return Math.max(0, indent - closes);
        }
        return indent;
    }

    /**
     * Returns the indent level after writing {@code line}.
     *
     * @param indent indent level that was used when writing
     * @param opens  open-brace count on the line
     * @param closes close-brace count on the line
     * @param line   trimmed source line
     * @return updated indent level
     */
    private static int postWriteIndent(int indent, int opens, int closes, @NotNull String line) {
        boolean startsWithClose = line.startsWith("}");
        if (opens > 0 && (opens != closes || startsWithClose)) {
            return indent + opens;
        }
        return indent;
    }

    /**
     * Writes {@code part} to {@code out} at the correct indentation for the expanded-block path,
     * then updates the indent tracker {@code indent[0]}.
     */
    private static void applyBraceIndent(@NotNull StringBuilder out, @NotNull String part,
                                         int opens, int closes, int indent) {
        int writeIndent = preWriteIndent(indent, opens, closes, part);
        out.append(" ".repeat(writeIndent * 4));
        out.append(part);
        out.append('\n');
    }

    /**
     * Returns the updated indent after writing a part of an expanded block.
     */
    private static int nextIndent(int indent, int opens, int closes, @NotNull String part) {
        int writeIndent = preWriteIndent(indent, opens, closes, part);
        return postWriteIndent(writeIndent, opens, closes, part);
    }

    /**
     * Checks if a line is a single-line block (e.g. {@code if (x) { foo(); }})
     * and expands it to multiple lines. Returns null if the line is not a single-line block.
     *
     * @param line trimmed source line
     * @return expanded multi-line string, or null if not applicable
     */
    private static @Nullable String expandSingleLineBlock(@NotNull String line) {
        int openBrace = line.indexOf('{');
        if (openBrace < 0) return null;

        int closeBrace = line.lastIndexOf('}');
        if (closeBrace <= openBrace) return null;
        if (closeBrace != line.length() - 1) return null;

        String before = line.substring(0, openBrace + 1).trim();
        String body = line.substring(openBrace + 1, closeBrace).trim();
        String after = "}";

        StringBuilder sb = new StringBuilder();
        sb.append(before).append('\n');
        if (!body.isEmpty()) {
            sb.append(body).append('\n');
        }
        sb.append(after).append('\n');
        return sb.toString();
    }

    /**
     * Merges consecutive if-blocks that share the exact same condition into a single block.
     * Comments between the blocks are moved inside the merged body to stay associated
     * with their respective code lines.
     *
     * <p>For example:</p>
     * <pre>
     * // @lumen:6: set entity powered to true
     * if (entity instanceof Creeper _cr) {
     *     _cr.setPowered(true);
     * }
     * // @lumen:7: set entity explosion radius to 6
     * if (entity instanceof Creeper _cr) {
     *     _cr.setExplosionRadius(6);
     * }
     * </pre>
     * <p>Becomes:</p>
     * <pre>
     * // @lumen:6: set entity powered to true
     * if (entity instanceof Creeper _cr) {
     *     _cr.setPowered(true);
     *     // @lumen:7: set entity explosion radius to 6
     *     _cr.setExplosionRadius(6);
     * }
     * </pre>
     *
     * @param formatted the already-formatted source
     * @return source with identical branches merged
     */
    private static @NotNull String mergeIdenticalBranches(@NotNull String formatted) {
        String[] lines = formatted.split("\n", -1);
        List<String> result = new ArrayList<>(lines.length);

        int i = 0;
        while (i < lines.length) {
            String trimmed = lines[i].trim();

            if (isIfBlockStart(trimmed)) {
                String indent = extractIndent(lines[i]);
                String bodyIndent = indent + "    ";

                List<String> bodyLines = new ArrayList<>();
                i++;
                i = collectBlockBody(lines, i, bodyLines);

                while (i < lines.length) {
                    int lookahead = i;
                    List<String> betweenComments = new ArrayList<>();

                    while (lookahead < lines.length) {
                        String lt = lines[lookahead].trim();
                        if (lt.isEmpty() || lt.startsWith("//")) {
                            if (lt.startsWith("//")) {
                                betweenComments.add(bodyIndent + lt);
                            }
                            lookahead++;
                        } else {
                            break;
                        }
                    }

                    if (lookahead < lines.length && lines[lookahead].trim().equals(trimmed)) {
                        bodyLines.addAll(betweenComments);
                        lookahead++;
                        List<String> nextBody = new ArrayList<>();
                        lookahead = collectBlockBody(lines, lookahead, nextBody);
                        bodyLines.addAll(nextBody);
                        i = lookahead;
                        continue;
                    }
                    break;
                }

                result.add(indent + trimmed);
                result.addAll(bodyLines);
                result.add(indent + "}");
            } else {
                result.add(lines[i]);
                i++;
            }
        }

        return String.join("\n", result);
    }

    /**
     * Collects the body lines of a brace-delimited block, starting after the opening {@code \{}.
     * Returns the index of the line after the closing {@code \}}.
     *
     * @param lines     all source lines
     * @param start     index to start scanning (first line inside the block)
     * @param bodyLines list to populate with body lines
     * @return the index immediately after the closing brace line
     */
    private static int collectBlockBody(@NotNull String[] lines, int start, @NotNull List<String> bodyLines) {
        int i = start;
        int braceDepth = 1;
        while (i < lines.length && braceDepth > 0) {
            String bl = lines[i].trim();
            for (int c = 0; c < bl.length(); c++) {
                if (bl.charAt(c) == '{') braceDepth++;
                else if (bl.charAt(c) == '}') braceDepth--;
            }
            if (braceDepth > 0) {
                bodyLines.add(lines[i]);
            }
            i++;
        }
        return i;
    }

    private static boolean isIfBlockStart(@NotNull String trimmed) {
        return trimmed.startsWith("if ") && trimmed.endsWith("{") && trimmed.contains("(");
    }

    private static @NotNull String extractIndent(@NotNull String line) {
        int idx = 0;
        while (idx < line.length() && line.charAt(idx) == ' ') idx++;
        return line.substring(0, idx);
    }

    /**
     * Replaces all {@code String.valueOf(expr)} calls with just {@code expr}.
     * Occurrences inside string or character literals are left untouched.
     *
     * @param source formatted source
     * @return source with String.valueOf unwrapped
     */
    private static @NotNull String stripStringValueOf(@NotNull String source) {
        return processLines(source, MiniJavaFormatter::stripStringValueOfFromLine);
    }

    private static @NotNull String stripStringValueOfFromLine(@NotNull String line) {
        if (line.trim().startsWith("//")) return line;
        final String marker = "String.valueOf(";
        StringBuilder out = new StringBuilder(line.length());
        int pos = 0;
        boolean inString = false;
        boolean inChar = false;

        while (pos < line.length()) {
            char c = line.charAt(pos);

            if (c == '\\' && (inString || inChar)) {
                out.append(c);
                if (pos + 1 < line.length()) {
                    out.append(line.charAt(pos + 1));
                    pos += 2;
                } else {
                    pos++;
                }
                continue;
            }

            if (c == '"' && !inChar) {
                inString = !inString;
                out.append(c);
                pos++;
                continue;
            }

            if (c == '\'' && !inString) {
                inChar = !inChar;
                out.append(c);
                pos++;
                continue;
            }

            if (inString || inChar) {
                out.append(c);
                pos++;
                continue;
            }

            if (line.startsWith(marker, pos)) {
                int argStart = pos + marker.length();
                int depth = 1;
                int i = argStart;
                boolean argInString = false;
                boolean argInChar = false;
                while (i < line.length() && depth > 0) {
                    char ac = line.charAt(i);
                    if (ac == '\\' && (argInString || argInChar)) {
                        i += 2;
                        continue;
                    }
                    if (ac == '"' && !argInChar) argInString = !argInString;
                    else if (ac == '\'' && !argInString) argInChar = !argInChar;
                    else if (!argInString && !argInChar) {
                        if (ac == '(') depth++;
                        else if (ac == ')') depth--;
                    }
                    i++;
                }
                out.append(line, argStart, i - 1);
                pos = i;
                continue;
            }

            out.append(c);
            pos++;
        }

        return out.toString();
    }

    /**
     * Removes all {@code .toString()} calls from the source.
     * Occurrences inside string or character literals are left untouched.
     *
     * @param source formatted source
     * @return source with .toString() calls removed
     */
    private static @NotNull String stripToString(@NotNull String source) {
        return processLines(source, MiniJavaFormatter::stripToStringFromLine);
    }

    private static @NotNull String stripToStringFromLine(@NotNull String line) {
        if (line.trim().startsWith("//")) return line;
        final String marker = ".toString()";
        StringBuilder out = new StringBuilder(line.length());
        int pos = 0;
        boolean inString = false;
        boolean inChar = false;

        while (pos < line.length()) {
            char c = line.charAt(pos);

            if (c == '\\' && (inString || inChar)) {
                out.append(c);
                if (pos + 1 < line.length()) {
                    out.append(line.charAt(pos + 1));
                    pos += 2;
                } else {
                    pos++;
                }
                continue;
            }

            if (c == '"' && !inChar) {
                inString = !inString;
                out.append(c);
                pos++;
                continue;
            }

            if (c == '\'' && !inString) {
                inChar = !inChar;
                out.append(c);
                pos++;
                continue;
            }

            if (inString || inChar) {
                out.append(c);
                pos++;
                continue;
            }

            if (line.startsWith(marker, pos)) {
                pos += marker.length();
                continue;
            }

            out.append(c);
            pos++;
        }

        return out.toString();
    }

    /**
     * Applies a per-line function to every line in {@code source}, reassembling with newlines.
     *
     * @param source   multi-line source
     * @param lineFunc function taking a single line and returning the transformed line
     * @return reassembled source
     */
    private static @NotNull String processLines(@NotNull String source,
                                                @NotNull LineTransformer lineFunc) {
        String[] lines = source.split("\n", -1);
        StringBuilder result = new StringBuilder(source.length());
        for (int i = 0; i < lines.length; i++) {
            result.append(lineFunc.transform(lines[i]));
            if (i < lines.length - 1) result.append('\n');
        }
        return result.toString();
    }

    /**
     * Removes all Java cast expressions such as {@code (long)}, {@code (Object)}, {@code (Player)}, etc.
     * Casts inside string or character literals are left untouched.
     * The resulting source may not compile and is intended for reading only.
     *
     * @param source formatted source
     * @return source with cast expressions removed
     */
    private static @NotNull String stripCasts(@NotNull String source) {
        return processLines(source, MiniJavaFormatter::stripCastsFromLine);
    }

    /**
     * Strips cast expressions from a single line, skipping any content inside string or char literals.
     *
     * @param line a single source line
     * @return the line with casts removed
     */
    private static @NotNull String stripCastsFromLine(@NotNull String line) {
        StringBuilder out = new StringBuilder(line.length());
        int pos = 0;
        boolean inString = false;
        boolean inChar = false;

        while (pos < line.length()) {
            char c = line.charAt(pos);

            if (c == '\\' && (inString || inChar)) {
                out.append(c);
                if (pos + 1 < line.length()) {
                    out.append(line.charAt(pos + 1));
                    pos += 2;
                } else {
                    pos++;
                }
                continue;
            }

            if (c == '"' && !inChar) {
                inString = !inString;
                out.append(c);
                pos++;
                continue;
            }

            if (c == '\'' && !inString) {
                inChar = !inChar;
                out.append(c);
                pos++;
                continue;
            }

            if (inString || inChar) {
                out.append(c);
                pos++;
                continue;
            }

            Matcher m = CAST_PATTERN.matcher(line);
            m.region(pos, line.length());
            if (m.lookingAt() && isCastContext(line, m.end())) {
                pos = m.end();
                if (pos < line.length() && line.charAt(pos) == ' ') {
                    pos++;
                }
                continue;
            }

            out.append(c);
            pos++;
        }

        return out.toString();
    }

    /**
     * Returns true if the position after a cast-shaped pattern is actually the start of a value
     * expression, confirming the match is a real cast and not an annotation argument or method call.
     * A real cast must be followed by an expression starter: a letter, digit, {@code (}, {@code "},
     * {@code '}, {@code -}, {@code !}, or {@code ~}. If the position is at end-of-line
     * or the next non-space character is {@code )}, {@code ,}, or {@code ;} it is not a cast.
     *
     * @param line       the source line
     * @param afterMatch index immediately after the closing {@code )} of the candidate cast
     * @return true if the context confirms a cast expression
     */
    private static boolean isCastContext(@NotNull String line, int afterMatch) {
        int i = afterMatch;
        while (i < line.length() && line.charAt(i) == ' ') i++;
        if (i >= line.length()) return false;
        char next = line.charAt(i);
        return Character.isLetterOrDigit(next)
                || next == '(' || next == '"' || next == '\''
                || next == '-' || next == '!' || next == '~';
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean endsWithBlankLine(@NotNull StringBuilder sb) {
        int len = sb.length();
        if (len < 2) return false;
        return sb.charAt(len - 1) == '\n' && sb.charAt(len - 2) == '\n';
    }

    /**
     * Pre-processes raw lines by splitting lines where block-level braces should
     * stand alone. A line like {@code "{ content;"} becomes two lines: {@code "{"}
     * and {@code "content;"}. Similarly {@code "content; }"} splits into
     * {@code "content;"} and {@code "}"}. This handles both single-line and
     * multi-line inline blocks.
     *
     * @param rawLines the original source lines
     * @return normalized array with split lines
     */
    private static @NotNull String[] normalizeLines(@NotNull String[] rawLines) {
        List<String> result = new ArrayList<>();
        for (String rawLine : rawLines) {
            String trimmed = rawLine.trim();
            if (trimmed.isEmpty()) {
                result.add(trimmed);
                continue;
            }
            splitBracketedLines(trimmed, result);
        }
        return result.toArray(new String[0]);
    }

    /**
     * Recursively splits a line at unmatched block-level braces.
     * If the line starts with {@code \{} and has more openers than closers, the
     * {@code \{} is emitted on its own and the rest is recursed. If the line ends
     * with {@code \}} and has more closers than openers, the trailing {@code \}}
     * is split off.
     *
     * @param line the trimmed source line
     * @param out  list to collect the resulting lines
     */
    private static void splitBracketedLines(@NotNull String line, @NotNull List<String> out) {
        int opens = countCodeBraces(line, '{');
        int closes = countCodeBraces(line, '}');

        if (line.startsWith("{") && opens > closes) {
            String rest = line.substring(1).trim();
            if (!rest.isEmpty()) {
                out.add("{");
                splitBracketedLines(rest, out);
                return;
            }
        }

        if (line.endsWith("}") && !isOnlyClosingBraces(line) && closes > opens) {
            String before = line.substring(0, line.length() - 1).trim();
            if (!before.isEmpty()) {
                splitBracketedLines(before, out);
                out.add("}");
                return;
            }
        }

        out.add(line);
    }

    /**
     * Checks if a line consists only of closing brace characters.
     *
     * @param line the trimmed source line
     * @return true if the line is two or more consecutive {@code '}'} only
     */
    private static boolean isOnlyClosingBraces(@NotNull String line) {
        if (line.length() < 2) return false;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) != '}') return false;
        }
        return true;
    }

    /**
     * Counts occurrences of the specified brace character in a line of code,
     * ignoring braces inside string literals, char literals, and comments.
     *
     * @param line  the source line
     * @param brace the brace character to count ({@code '&#123;'} or {@code '&#125;'})
     * @return the number of code-level brace occurrences
     */
    private static int countCodeBraces(@NotNull String line, char brace) {
        String trimmed = line.trim();
        if (trimmed.startsWith("//")) return 0;
        int count = 0;
        boolean inString = false;
        boolean inChar = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\' && (inString || inChar) && i + 1 < line.length()) {
                i++;
                continue;
            }
            if (c == '"' && !inChar) {
                inString = !inString;
            } else if (c == '\'' && !inString) {
                inChar = !inChar;
            } else if (!inString && !inChar && c == brace) {
                count++;
            }
        }
        return count;
    }

    @FunctionalInterface
    private interface LineTransformer {
        @NotNull String transform(@NotNull String line);
    }
}
