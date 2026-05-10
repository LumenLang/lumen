package dev.lumenlang.build.source;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Resolves a class's compiled internal name to its {@code .java} source file
 * by checking every configured source root in declaration order.
 */
public final class SourceLocator {

    private SourceLocator() {
    }

    /**
     * Looks up a {@code .java} file for the given outer class.
     *
     * @param ownerInternalName slash-form internal name of the top-level class,
     *                          e.g. {@code com/example/foo/MyHandlers}
     * @param sourceRoots       source-root directories, checked in order
     * @return the resolved path, or null when no source root contains it
     */
    public static @Nullable Path locate(@NotNull String ownerInternalName, @NotNull List<Path> sourceRoots) {
        String relative = topLevelOf(ownerInternalName) + ".java";
        for (Path root : sourceRoots) {
            Path candidate = root.resolve(relative);
            if (Files.isRegularFile(candidate)) return candidate;
        }
        return null;
    }

    /**
     * Strips any nested-class suffixes ({@code $Inner}) so the lookup matches the
     * containing source file.
     */
    private static @NotNull String topLevelOf(@NotNull String internalName) {
        int dollar = internalName.indexOf('$');
        return dollar < 0 ? internalName : internalName.substring(0, dollar);
    }
}
