package dev.lumenlang.lumen.api.inject.index;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Preserved source view of one annotated handler method. The build plugin
 * parses the original source with Vanta and ships pre-extracted structured
 * fields here so the runtime never has to parse Java.
 *
 * @param owner            JVM internal name of the declaring class
 * @param method           method name
 * @param descriptor       JVMS method descriptor
 * @param imports          fully qualified class names imported by the source file
 * @param compileSource    raw source text of the {@code // lumen:compile} section, or null when none
 * @param runtimeBodyLines runtime body broken into top-level Java statements, in source order
 * @param returnExpression for single-{@code return} runtime bodies, the expression after {@code return} and before the trailing {@code ;}, otherwise null
 * @param alwaysThrows     true when the runtime body's only top-level statement is a {@code throw}
 */
public record SidecarEntry(@NotNull String owner, @NotNull String method, @NotNull String descriptor, @NotNull List<String> imports, @Nullable String compileSource, @NotNull List<String> runtimeBodyLines, @Nullable String returnExpression, boolean alwaysThrows) {
}
