package dev.lumenlang.build.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.language.jvm.tasks.ProcessResources;
import org.jetbrains.annotations.NotNull;

/**
 * Apply with {@code id 'dev.lumenlang.lumen-build'} on any addon project that
 * uses the Lumen annotation API. The plugin registers a {@code lumenBuild}
 * task that writes {@code META-INF/lumen/} sidecars into the addon's
 * resources output between {@code compileJava} and {@code jar}.
 */
public final class LumenGradlePlugin implements Plugin<Project> {

    private static final String TASK_NAME = "lumenBuild";

    @Override
    public void apply(@NotNull Project project) {
        project.getPlugins().apply("java");

        project.getTasks().withType(JavaCompile.class).configureEach(t -> t.getOptions().getCompilerArgs().add("-parameters"));

        SourceSet main = project.getExtensions().getByType(SourceSetContainer.class).getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        var taskProvider = project.getTasks().register(TASK_NAME, LumenBuildTask.class, task -> {
            task.setGroup("build");
            task.setDescription("Scans annotated Lumen handlers and writes META-INF/lumen sidecars.");
            task.getClassesDir().set(main.getJava().getDestinationDirectory());
            task.getSourceDirs().from(main.getJava().getSrcDirs());
            task.getResourcesDir().set(project.getLayout().getBuildDirectory().dir("resources/main"));
            task.dependsOn(main.getCompileJavaTaskName());
        });

        project.getTasks().named(main.getProcessResourcesTaskName(), ProcessResources.class, t -> t.dependsOn(taskProvider));
        project.getTasks().withType(Jar.class).configureEach(jar -> jar.dependsOn(taskProvider));
    }
}
