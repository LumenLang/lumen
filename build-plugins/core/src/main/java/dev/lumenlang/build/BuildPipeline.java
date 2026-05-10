package dev.lumenlang.build;

import dev.lumenlang.build.emit.map.IndexMapper;
import dev.lumenlang.build.emit.map.SidecarMapper;
import dev.lumenlang.build.emit.write.HandlersIndexWriter;
import dev.lumenlang.build.emit.write.SourceSidecarWriter;
import dev.lumenlang.build.io.BuildInputs;
import dev.lumenlang.build.result.BuildResult;
import dev.lumenlang.build.result.Diagnostic;
import dev.lumenlang.build.result.Severity;
import dev.lumenlang.build.scan.ClassScanner;
import dev.lumenlang.build.scan.handler.ScannedHandler;
import dev.lumenlang.build.source.HandlerSourceParser;
import dev.lumenlang.build.source.ParsedHandlerSource;
import dev.lumenlang.build.source.SourceLocator;
import dev.lumenlang.build.validate.InjectTypeValidator;
import dev.lumenlang.build.validate.PhaseScopeValidator;
import dev.lumenlang.lumen.api.inject.index.IndexedHandler;
import dev.lumenlang.lumen.api.inject.index.SidecarEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * End-to-end orchestration of the build pipeline. Each phase is a static
 * call into a peer package; this class only wires inputs and outputs and
 * collects diagnostics.
 */
public final class BuildPipeline {

    private BuildPipeline() {
    }

    public static @NotNull BuildResult execute(@NotNull BuildInputs inputs) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        List<ScannedHandler> scanned;
        try {
            scanned = ClassScanner.scan(inputs.classesDir());
        } catch (IOException e) {
            return new BuildResult(0, 0, List.of(new Diagnostic(Severity.ERROR, "Failed to scan classes directory: " + e.getMessage(), inputs.classesDir(), 0, 0)));
        }
        inputs.logger().info("Lumen build: scanned " + scanned.size() + " annotated handler(s)");
        if (scanned.isEmpty()) return new BuildResult(0, 0, List.copyOf(diagnostics));

        Map<ScannedHandler, ParsedHandlerSource> parsedByHandler = parseSources(scanned, inputs, diagnostics);

        for (Map.Entry<ScannedHandler, ParsedHandlerSource> entry : parsedByHandler.entrySet()) {
            diagnostics.addAll(PhaseScopeValidator.validate(entry.getKey(), entry.getValue()));
            diagnostics.addAll(InjectTypeValidator.validate(entry.getKey(), entry.getValue(), inputs.bindingTypes()));
        }

        if (hasErrors(diagnostics) || inputs.validateOnly()) {
            return new BuildResult(0, 0, List.copyOf(diagnostics));
        }

        int sourceEntries = writeSidecars(scanned, parsedByHandler, inputs, diagnostics);
        return new BuildResult(scanned.size(), sourceEntries, List.copyOf(diagnostics));
    }

    /**
     * Locates and parses the source file for every scanned handler. Failures
     * become error diagnostics; the handler is dropped from the returned map.
     */
    private static @NotNull Map<ScannedHandler, ParsedHandlerSource> parseSources(@NotNull List<ScannedHandler> scanned, @NotNull BuildInputs inputs, @NotNull List<Diagnostic> diagnostics) {
        Map<ScannedHandler, ParsedHandlerSource> map = new LinkedHashMap<>();
        for (ScannedHandler handler : scanned) {
            ParsedHandlerSource parsed = parseSource(handler, inputs, diagnostics);
            if (parsed != null) map.put(handler, parsed);
        }
        return map;
    }

    private static @Nullable ParsedHandlerSource parseSource(@NotNull ScannedHandler handler, @NotNull BuildInputs inputs, @NotNull List<Diagnostic> diagnostics) {
        var sourceFile = SourceLocator.locate(handler.ownerInternalName(), inputs.sourceDirs());
        if (sourceFile == null) {
            diagnostics.add(new Diagnostic(Severity.ERROR, "Could not locate source file for class '" + handler.ownerInternalName() + "'", null, 0, 0));
            return null;
        }
        try {
            String simpleName = simpleName(handler.ownerInternalName());
            int paramCount = countParams(handler.methodDescriptor());
            ParsedHandlerSource parsed = HandlerSourceParser.parse(sourceFile, simpleName, handler.methodName(), paramCount);
            if (parsed == null) {
                diagnostics.add(new Diagnostic(Severity.ERROR, "Could not match handler method '" + handler.methodName() + "' in source file", sourceFile, 0, 0));
            }
            return parsed;
        } catch (IOException e) {
            diagnostics.add(new Diagnostic(Severity.ERROR, "Failed to read source file: " + e.getMessage(), sourceFile, 0, 0));
            return null;
        }
    }

    /**
     * Writes the handlers index and the gzipped source sidecar. Returns the
     * count of source entries actually emitted.
     */
    private static int writeSidecars(@NotNull List<ScannedHandler> scanned, @NotNull Map<ScannedHandler, ParsedHandlerSource> parsedByHandler, @NotNull BuildInputs inputs, @NotNull List<Diagnostic> diagnostics) {
        List<IndexedHandler> indexed = new ArrayList<>(scanned.size());
        List<SidecarEntry> entries = new ArrayList<>(scanned.size());
        for (ScannedHandler handler : scanned) {
            indexed.add(IndexMapper.map(handler));
            ParsedHandlerSource parsed = parsedByHandler.get(handler);
            if (parsed != null) entries.add(SidecarMapper.map(handler, parsed));
        }
        try {
            HandlersIndexWriter.write(inputs.resourcesDir(), indexed);
            SourceSidecarWriter.write(inputs.resourcesDir(), entries);
        } catch (IOException e) {
            diagnostics.add(new Diagnostic(Severity.ERROR, "Failed to write META-INF/lumen sidecars: " + e.getMessage(), inputs.resourcesDir(), 0, 0));
            return 0;
        }
        return entries.size();
    }


    private static boolean hasErrors(@NotNull List<Diagnostic> diagnostics) {
        for (Diagnostic d : diagnostics) if (d.severity() == Severity.ERROR) return true;
        return false;
    }

    private static @NotNull String simpleName(@NotNull String internalName) {
        int slash = internalName.lastIndexOf('/');
        String trailing = slash < 0 ? internalName : internalName.substring(slash + 1);
        int dollar = trailing.lastIndexOf('$');
        return dollar < 0 ? trailing : trailing.substring(dollar + 1);
    }

    /**
     * Counts the parameters declared in a JVMS method descriptor.
     */
    private static int countParams(@NotNull String descriptor) {
        if (!descriptor.startsWith("(")) throw new IllegalArgumentException("Bad method descriptor: " + descriptor);
        int count = 0;
        int i = 1;
        while (i < descriptor.length() && descriptor.charAt(i) != ')') {
            char c = descriptor.charAt(i);
            if (c == 'L') {
                int end = descriptor.indexOf(';', i);
                if (end < 0) throw new IllegalArgumentException("Bad method descriptor: " + descriptor);
                count++;
                i = end + 1;
            } else if (c == '[') {
                i++;
            } else {
                count++;
                i++;
            }
        }
        return count;
    }
}
