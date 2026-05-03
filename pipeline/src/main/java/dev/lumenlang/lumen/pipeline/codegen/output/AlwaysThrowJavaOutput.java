package dev.lumenlang.lumen.pipeline.codegen.output;

import dev.lumenlang.lumen.api.codegen.JavaOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * {@link JavaOutput} that throws a configured {@link RuntimeException} on every call.
 */
public final class AlwaysThrowJavaOutput implements JavaOutput {

    private final RuntimeException exception;

    public AlwaysThrowJavaOutput(@NotNull RuntimeException exception) {
        this.exception = exception;
    }

    @Override
    public void line(@NotNull String code) {
        throw exception;
    }

    @Override
    public int lineNum() {
        throw exception;
    }

    @Override
    public void insertLine(int index, @NotNull String code) {
        throw exception;
    }

    @Override
    public void taggedLine(@Nullable String tag, @NotNull String code) {
        throw exception;
    }
}
