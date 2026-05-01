package dev.lumenlang.console.live;

import dev.lumenlang.console.element.Element;
import dev.lumenlang.console.element.Renderer;
import dev.lumenlang.console.style.Ansi;
import dev.lumenlang.console.terminal.TerminalCapabilities;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.function.IntFunction;

/**
 * Drives a single multi-line rendering surface that updates in place. Each tick clears the
 * previously printed lines using ANSI cursor codes and prints a fresh frame. Suitable for progress
 * dashboards, spinners, and live diagnostics that need to refresh many times per second without
 * scrolling the terminal.
 *
 * <p>Callers should consult {@link TerminalCapabilities#supportsCursor()} before instantiating
 * this class and fall back to a non-live render path when the terminal does not support cursor
 * movement. As a safety net, when the terminal cannot be detected as a TTY, this class silently
 * suppresses every frame except the last one (printed at {@link #finish()}) so consoles like
 * IntelliJ's Run window do not stack frame after frame.
 *
 * <p>The element supplied at each frame may have any size; the renderer remembers the line count
 * of the previous frame so it knows how far up to move the cursor before redrawing.
 *
 * <p>This class writes directly to a {@link PrintStream}. It does not coordinate with concurrent
 * loggers, so it works best when no other code is writing to the same stream during the render.
 * The future {@code ConsoleManager} will provide that coordination.
 */
@SuppressWarnings("unused")
public final class LiveRender {

    private final @NotNull PrintStream out;
    private final boolean liveCapable;
    private @NotNull String[] previousLines = new String[0];
    private boolean started;
    private @Nullable Element lastFrame;
    private int lastFrameDefaultWidth;
    private int lastFrameDefaultHeight;

    /**
     * Creates a renderer that writes to the given print stream. Live updates are auto-disabled
     * when {@link TerminalCapabilities#supportsCursor()} returns false.
     */
    public LiveRender(@NotNull PrintStream out) {
        this(out, TerminalCapabilities.supportsCursor());
    }

    /**
     * Creates a renderer with explicit live capability. Use the auto-detecting constructor in
     * regular code; this overload exists for tests and edge-case overrides.
     */
    public LiveRender(@NotNull PrintStream out, boolean liveCapable) {
        this.out = out;
        this.liveCapable = liveCapable;
    }

    /**
     * Creates a renderer that writes to {@link System#out}.
     */
    public static @NotNull LiveRender stdout() {
        return new LiveRender(System.out);
    }

    /**
     * Renders a single frame using the current terminal columns as the flex-width default and a
     * height of 1 as the flex-height default.
     */
    public synchronized void draw(@NotNull Element root) {
        draw(root, TerminalCapabilities.columns(), 1);
    }

    /**
     * Renders a single frame, replacing the previous one. When the terminal does not support
     * cursor movement, the frame is buffered and only the most recent frame is printed at
     * {@link #finish()}.
     */
    public synchronized void draw(@NotNull Element root, int defaultWidth, int defaultHeight) {
        if (!liveCapable) {
            lastFrame = root;
            lastFrameDefaultWidth = defaultWidth;
            lastFrameDefaultHeight = defaultHeight;
            return;
        }
        int w = root.width() < 0 ? defaultWidth : root.width();
        int h = root.height() < 0 ? defaultHeight : root.height();
        String[] lines = root.render(w, h);
        StringBuilder sb = new StringBuilder();
        if (!started) {
            for (int i = 0; i < lines.length; i++) {
                sb.append(lines[i]);
                if (i < lines.length - 1) sb.append('\n');
            }
        } else {
            int prevHeight = previousLines.length;
            int newHeight = lines.length;
            int totalHeight = Math.max(prevHeight, newHeight);
            sb.append('\r');
            if (prevHeight > 1) sb.append(Ansi.CSI).append(prevHeight - 1).append('A');
            for (int i = 0; i < totalHeight; i++) {
                String newLine = i < newHeight ? lines[i] : "";
                String oldLine = i < prevHeight ? previousLines[i] : "";
                if (!newLine.equals(oldLine)) {
                    sb.append('\r');
                    sb.append(newLine);
                    sb.append(Ansi.CSI).append("0K");
                }
                if (i < totalHeight - 1) sb.append(Ansi.CSI).append("1B");
            }
            int cursorRow = totalHeight - 1;
            int targetRow = newHeight - 1;
            if (cursorRow > targetRow) sb.append(Ansi.CSI).append(cursorRow - targetRow).append('A');
        }
        out.print(sb);
        out.flush();
        previousLines = lines;
        started = true;
    }

    /**
     * Releases the cursor below the last rendered frame so subsequent log output appears on its
     * own line. When live updates were suppressed because the terminal lacks cursor support, the
     * last buffered frame is printed once now.
     */
    public synchronized void finish() {
        if (!liveCapable && lastFrame != null) {
            out.println(Renderer.render(lastFrame, lastFrameDefaultWidth, lastFrameDefaultHeight));
            out.flush();
            lastFrame = null;
        }
        if (!started) return;
        started = false;
        previousLines = new String[0];
    }

    /**
     * Runs an animation that produces one element per frame index until the supplier returns
     * {@code null}. Sleeps {@code intervalMillis} between frames.
     *
     * @param frames         supplier called once per tick with the current frame index
     * @param intervalMillis milliseconds between frames
     * @param defaultWidth   used when the element has flexible width
     * @param defaultHeight  used when the element has flexible height
     */
    public void animate(@NotNull IntFunction<Element> frames, long intervalMillis, int defaultWidth, int defaultHeight) {
        int i = 0;
        while (true) {
            Element frame = frames.apply(i);
            if (frame == null) break;
            draw(frame, defaultWidth, defaultHeight);
            try {
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            i++;
        }
        finish();
    }
}
