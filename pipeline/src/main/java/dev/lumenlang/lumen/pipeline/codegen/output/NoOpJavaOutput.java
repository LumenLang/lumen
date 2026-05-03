package dev.lumenlang.lumen.pipeline.codegen.output;

import dev.lumenlang.lumen.api.codegen.JavaOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * {@link JavaOutput} that discards every emitted line.
 */
public final class NoOpJavaOutput implements JavaOutput {

    public static final NoOpJavaOutput INSTANCE = new NoOpJavaOutput();

    @Override
    public void line(@NotNull String code) {
    }

    @Override
    public int lineNum() {
        return 0;
    }

    @Override
    public void insertLine(int index, @NotNull String code) {
    }

    @Override
    public void taggedLine(@Nullable String tag, @NotNull String code) {
    }
}
