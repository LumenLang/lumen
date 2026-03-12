package dev.lumenlang.lumen.api.pattern;

import org.jetbrains.annotations.NotNull;

/**
 * Utility for building multi-line Lumen script examples with explicit indentation levels.
 *
 * <p>Instead of manually escaping newlines and indentation:
 * <pre>{@code
 * .example("on join:\\n    send player \"Welcome!\"")
 * }</pre>
 *
 * <p>You can write:
 * <pre>{@code
 * .example(LumaExample.of(
 *     LumaExample.top("on join:"),
 *     LumaExample.secondly("send player \"Welcome!\"")
 * ))
 * }</pre>
 *
 * <p>For deeper nesting:
 * <pre>{@code
 * .example(LumaExample.of(
 *     LumaExample.top("on join:"),
 *     LumaExample.secondly("if player is sneaking:"),
 *     LumaExample.thirdly("send player \"Sneaky!\""),
 *     LumaExample.secondly("else:"),
 *     LumaExample.thirdly("send player \"Normal\"")
 * ))
 * }</pre>
 *
 * <p>Supports up to four indent levels (top, secondly, thirdly, fourthly), examples must not exceed 3 levels of nesting.
 */
@SuppressWarnings("unused")
public final class LumaExample {

    private static final String INDENT = "    ";

    private LumaExample() {
    }

    /**
     * Joins pre-indented lines into a single example string.
     *
     * <p>Each line should be produced by {@link #top(String)}, {@link #secondly(String)},
     * {@link #thirdly(String)}, or {@link #fourthly(String)}.
     *
     * @param lines one or more pre-indented lines
     * @return the formatted example string
     */
    public static @NotNull String of(@NotNull String... lines) {
        return String.join("\n", lines);
    }

    /**
     * Returns the line with no indentation (top level, depth 0).
     *
     * @param line the script line
     * @return the line as-is
     */
    public static @NotNull String top(@NotNull String line) {
        return line;
    }

    /**
     * Returns the line indented once (depth 1, 4 spaces).
     *
     * @param line the script line
     * @return the line prefixed with 4 spaces
     */
    public static @NotNull String secondly(@NotNull String line) {
        return INDENT + line;
    }

    /**
     * Returns the line indented twice (depth 2, 8 spaces).
     *
     * @param line the script line
     * @return the line prefixed with 8 spaces
     */
    public static @NotNull String thirdly(@NotNull String line) {
        return INDENT + INDENT + line;
    }

    /**
     * Returns the line indented three times (depth 3, 12 spaces).
     *
     * @param line the script line
     * @return the line prefixed with 12 spaces
     */
    public static @NotNull String fourthly(@NotNull String line) {
        return INDENT + INDENT + INDENT + line;
    }
}
