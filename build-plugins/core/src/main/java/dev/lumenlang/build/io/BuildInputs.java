package dev.lumenlang.build.io;

import dev.lumenlang.build.validate.inject.BindingTypeTable;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

/**
 * Paths and options the build pipeline consumes. Shims fill this from the
 * build system's project model.
 *
 * @param classesDir   compiled {@code .class} root
 * @param sourceDirs   {@code .java} source roots, in resolution order
 * @param resourcesDir output root for {@code META-INF/lumen/...} sidecars
 * @param bindingTypes maps a type-binding id to the JVMS field descriptor it produces; consulted by the inject-type validator. Pass an empty table to skip strict checks.
 * @param validateOnly skip class rewrite and sidecar emit; only run validation
 * @param logger       log sink wired to the build system
 */
public record BuildInputs(@NotNull Path classesDir, @NotNull List<Path> sourceDirs, @NotNull Path resourcesDir,
                          @NotNull BindingTypeTable bindingTypes, boolean validateOnly, @NotNull BuildLogger logger) {
}
