package dev.lumenlang.lumen.headless.sim.debug;

import dev.lumenlang.console.UIUtils;
import dev.lumenlang.console.element.Renderer;
import dev.lumenlang.console.style.Color;
import dev.lumenlang.lumen.pipeline.language.simulator.debug.DebugSink;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Console-utils backed sink that colorises lines by their leading marker and indents each line by
 * its depth. Default routes formatted output to {@code System.out}.
 *
 * <p>Markers honoured: {@code +} (admitted candidate, mint), {@code -} (rejected, slate),
 * {@code #} (ranking entry, sky), {@code !} (issue or fallback, warm yellow). Lines without a
 * marker render in {@link Color#BONE}.
 */
public final class TextDebugSink implements DebugSink {

    private final Consumer<String> writer;

    public TextDebugSink(@NotNull Consumer<String> writer) {
        this.writer = writer;
    }

    /**
     * Sink that prints to {@code System.out}.
     */
    public static @NotNull TextDebugSink stdout() {
        return new TextDebugSink(System.out::println);
    }

    @Override
    public void write(int depth, @NotNull String line) {
        StringBuilder indent = new StringBuilder(depth * 2);
        for (int i = 0; i < depth; i++) indent.append("  ");
        Color color = colorFor(line);
        String body = indent + line;
        writer.accept(Renderer.render(UIUtils.text(body).fg(color)));
    }

    private static @NotNull Color colorFor(@NotNull String line) {
        String trimmed = line.stripLeading();
        if (trimmed.startsWith("+ ")) return Color.MINT;
        if (trimmed.startsWith("- ")) return Color.SLATE;
        if (trimmed.startsWith("#")) return Color.SKY;
        if (trimmed.startsWith("!")) return Color.WARM_YELLOW;
        return Color.BONE;
    }
}
