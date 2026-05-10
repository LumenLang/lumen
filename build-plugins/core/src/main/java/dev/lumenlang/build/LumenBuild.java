package dev.lumenlang.build;

import dev.lumenlang.build.io.BuildInputs;
import dev.lumenlang.build.result.BuildResult;
import org.jetbrains.annotations.NotNull;

/**
 * Entry point for the Lumen addon build pipeline. Build-system shims gather
 * paths and a logger into {@link BuildInputs}, then call {@link #run}.
 */
public final class LumenBuild {

    private LumenBuild() {
    }

    public static @NotNull BuildResult run(@NotNull BuildInputs inputs) {
        return BuildPipeline.execute(inputs);
    }
}
