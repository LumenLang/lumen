package dev.lumenlang.build.gradle;

import dev.lumenlang.build.io.BuildLogger;
import org.gradle.api.Task;
import org.jetbrains.annotations.NotNull;

/**
 * Adapts {@link BuildLogger} onto a Gradle task's logger.
 */
public final class GradleBridgeLogger implements BuildLogger {

    private final @NotNull Task task;

    public GradleBridgeLogger(@NotNull Task task) {
        this.task = task;
    }

    @Override
    public void info(@NotNull String message) {
        task.getLogger().info("[Lumen] {}", message);
    }

    @Override
    public void warn(@NotNull String message) {
        task.getLogger().warn("[Lumen] {}", message);
    }

    @Override
    public void error(@NotNull String message) {
        task.getLogger().error("[Lumen] {}", message);
    }
}
