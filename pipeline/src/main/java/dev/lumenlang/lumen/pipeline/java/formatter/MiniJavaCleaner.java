package dev.lumenlang.lumen.pipeline.java.formatter;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A lightweight Java code cleaner providing post-processing transformations.
 */
@SuppressWarnings("unused")
public final class MiniJavaCleaner {

    private static final Pattern CAST_PATTERN = Pattern.compile("\\((?:int|long|double|float|short|byte|char|boolean|[A-Z]\\w*(?:\\.\\w+)*(?:<[^)]*>)?(?:\\[])*)\\)");

    /**
     * Merges identical branches and additionally removes all cast expressions,
     * {@code String.valueOf()}, and {@code .toString()} calls.
     * The resulting source is intended for human reading only and may not compile.
     *
     * @param source Java source
     * @return source code that's easier to read
     */
    public static @NotNull String formatReadable(@NotNull String source) {
        return "// Readable view (branches merged, casts removed, no String.valueOf or .toString).\n"
                + "// This file is for reading only, it may not compile.\n\n" + stripCasts(stripToString(stripStringValueOf(mergeIdenticalBranches(source))));
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
        return processLines(source, MiniJavaCleaner::stripStringValueOfFromLine);
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
        return processLines(source, MiniJavaCleaner::stripToStringFromLine);
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
        return processLines(source, MiniJavaCleaner::stripCastsFromLine);
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

    @FunctionalInterface
    private interface LineTransformer {
        @NotNull String transform(@NotNull String line);
    }
}
