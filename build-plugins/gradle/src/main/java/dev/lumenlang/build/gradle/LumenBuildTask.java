package dev.lumenlang.build.gradle;

import dev.lumenlang.build.LumenBuild;
import dev.lumenlang.build.io.BuildInputs;
import dev.lumenlang.build.result.BuildResult;
import dev.lumenlang.build.result.Diagnostic;
import dev.lumenlang.build.result.Severity;
import dev.lumenlang.build.validate.inject.BindingTypeTable;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Scans the addon's compiled classes for annotated handlers and writes the
 * {@code META-INF/lumen} sidecars Lumen reads at runtime.
 */
public abstract class LumenBuildTask extends DefaultTask {

    @InputDirectory
    public abstract DirectoryProperty getClassesDir();

    @InputFiles
    public abstract ConfigurableFileCollection getSourceDirs();

    @OutputDirectory
    public abstract DirectoryProperty getResourcesDir();

    @TaskAction
    public void run() {
        Path classesDir = getClassesDir().get().getAsFile().toPath();
        Path resourcesDir = getResourcesDir().get().getAsFile().toPath();
        List<Path> sourceDirs = new ArrayList<>();
        for (File f : getSourceDirs().getFiles()) sourceDirs.add(f.toPath());

        BuildResult result = LumenBuild.run(new BuildInputs(classesDir, sourceDirs, resourcesDir, new BindingTypeTable(Map.of()), false, new GradleBridgeLogger(this)));

        boolean hasError = false;
        for (Diagnostic d : result.diagnostics()) {
            String message = format(d);
            if (d.severity() == Severity.ERROR) {
                getLogger().error("[Lumen] {}", message);
                hasError = true;
            } else {
                getLogger().warn("[Lumen] {}", message);
            }
        }
        getLogger().lifecycle("[Lumen] processed={} sidecarEntries={}", result.rewrittenClasses(), result.sourceEntriesEmitted());
        if (hasError) {
            throw new GradleException("Lumen build failed; see diagnostics above.");
        }
    }

    private static @NotNull String format(@NotNull Diagnostic d) {
        StringBuilder sb = new StringBuilder();
        if (d.file() != null) sb.append(d.file()).append(':');
        if (d.line() > 0) sb.append(d.line()).append(':');
        if (d.column() > 0) sb.append(d.column()).append(':');
        if (!sb.isEmpty()) sb.append(' ');
        sb.append(d.message());
        return sb.toString();
    }
}
