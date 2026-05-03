package dev.lumenlang.lumen.pipeline.codegen.source;

import dev.lumenlang.lumen.api.codegen.source.SourceMap;
import dev.lumenlang.lumen.pipeline.language.tokenization.Line;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SourceMapImpl implements SourceMap {

    private final String fullSource;
    private final int totalLines;
    private final Map<Integer, String> contentByLine;

    public SourceMapImpl(@NotNull String fullSource) {
        this.fullSource = fullSource;
        String[] split = fullSource.split("\\R", -1);
        this.totalLines = split.length;
        this.contentByLine = new HashMap<>(split.length);
        for (int i = 0; i < split.length; i++) {
            contentByLine.put(i + 1, split[i]);
        }
    }

    public SourceMapImpl(@NotNull String fullSource, @NotNull List<Line> tokenizedLines) {
        this.fullSource = fullSource;
        this.totalLines = fullSource.split("\\R", -1).length;
        this.contentByLine = new HashMap<>(tokenizedLines.size());
        for (Line l : tokenizedLines) {
            contentByLine.put(l.lineNumber(), l.raw());
        }
    }

    @Override
    public @NotNull String rawAt(int line) {
        if (line < 1 || line > totalLines) {
            throw new IndexOutOfBoundsException("line " + line + " outside [1, " + totalLines + "]");
        }
        return contentByLine.getOrDefault(line, "");
    }

    @Override
    public @NotNull List<String> rawRange(int from, int to) {
        if (from > to) throw new IllegalArgumentException("from " + from + " > to " + to);
        if (from < 1 || to > totalLines) {
            throw new IndexOutOfBoundsException("range [" + from + ", " + to + "] outside [1, " + totalLines + "]");
        }
        List<String> out = new ArrayList<>(to - from + 1);
        for (int i = from; i <= to; i++) {
            out.add(contentByLine.getOrDefault(i, ""));
        }
        return Collections.unmodifiableList(out);
    }

    @Override
    public boolean hasLine(int line) {
        return line >= 1 && line <= totalLines;
    }

    @Override
    public int lineCount() {
        return totalLines;
    }

    @Override
    public @NotNull String fullSource() {
        return fullSource;
    }
}
