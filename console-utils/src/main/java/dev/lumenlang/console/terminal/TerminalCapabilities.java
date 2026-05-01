package dev.lumenlang.console.terminal;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * Best-effort introspection of the current terminal. None of these are guaranteed: the JVM does
 * not expose terminal capabilities reliably across platforms, so each method falls back to a
 * conservative default when uncertain.
 */
public final class TerminalCapabilities {

    private TerminalCapabilities() {
    }

    /**
     * Returns {@code true} when standard output is likely a TTY. Honors {@code System.console()}
     * first; falls back to checking the {@code TERM} environment variable, which a real terminal
     * almost always sets and which survives wrappers like gradle that hide {@code System.console()}
     * behind their own streams.
     */
    public static boolean isTty() {
        if (System.console() != null) return true;
        String term = System.getenv("TERM");
        return term != null && !term.isEmpty() && !"dumb".equals(term);
    }

    /**
     * Returns {@code true} when the terminal likely supports 24-bit color. Honors {@code COLORTERM}
     * if set; falls back to {@link #isTty()} otherwise.
     */
    public static boolean supportsTruecolor() {
        String colorTerm = System.getenv("COLORTERM");
        if (colorTerm != null && (colorTerm.contains("truecolor") || colorTerm.contains("24bit"))) return true;
        String term = System.getenv("TERM");
        if (term != null && term.contains("256color")) return true;
        return isTty();
    }

    /**
     * Returns {@code true} when cursor movement escapes are likely honored. Same heuristics as
     * truecolor support, since both indicate a real terminal.
     */
    public static boolean supportsCursor() {
        return isTty();
    }

    /**
     * Returns the terminal column count, falling back to {@code 80} when undetectable. Honors
     * {@code COLUMNS} env first, then {@code stty size}.
     */
    public static int columns() {
        Integer env = parseEnv("COLUMNS");
        if (env != null) return env;
        int[] size = sttySize();
        return size != null ? size[1] : 80;
    }

    /**
     * Returns the terminal row count, falling back to {@code 24} when undetectable. Honors
     * {@code LINES} env first, then {@code stty size}.
     */
    public static int rows() {
        Integer env = parseEnv("LINES");
        if (env != null) return env;
        int[] size = sttySize();
        return size != null ? size[0] : 24;
    }

    private static @Nullable Integer parseEnv(@NotNull String name) {
        String v = System.getenv(name);
        if (v == null) return null;
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int @Nullable [] sttySize() {
        try {
            ProcessBuilder pb = new ProcessBuilder("stty", "size");
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
            Process p = pb.start();
            byte[] out = p.getInputStream().readAllBytes();
            if (!p.waitFor(500, TimeUnit.MILLISECONDS)) {
                p.destroyForcibly();
                return null;
            }
            String[] parts = new String(out).trim().split("\\s+");
            if (parts.length < 2) return null;
            return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
        } catch (Exception e) {
            return null;
        }
    }
}
