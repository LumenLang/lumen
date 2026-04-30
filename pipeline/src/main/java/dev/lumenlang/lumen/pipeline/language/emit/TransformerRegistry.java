package dev.lumenlang.lumen.pipeline.language.emit;

import dev.lumenlang.lumen.api.codegen.CodegenAccess;
import dev.lumenlang.lumen.api.emit.transform.CodeTransformer;
import dev.lumenlang.lumen.api.emit.transform.TaggedLine;
import dev.lumenlang.lumen.api.emit.transform.TransformContext;
import dev.lumenlang.lumen.pipeline.codegen.CodegenContext;
import dev.lumenlang.lumen.pipeline.java.JavaBuilder;
import dev.lumenlang.lumen.pipeline.java.compiled.ClassBuilder;
import dev.lumenlang.lumen.pipeline.logger.LumenLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Stores registered code transformers and runs them against emitted Java code.
 *
 * <p>Transformers are registered through the API. After code generation completes
 * for a script, all registered transformers are run to inspect and modify the
 * emitted lines.
 *
 * <p>This is an experimental feature.
 */
public final class TransformerRegistry {

    private static final int MAX_PASSES = 50;

    private static TransformerRegistry INSTANCE;

    private final List<CodeTransformer> transformers = new CopyOnWriteArrayList<>();

    /**
     * Sets the global singleton instance.
     *
     * @param instance the registry to use
     */
    public static void instance(@NotNull TransformerRegistry instance) {
        INSTANCE = instance;
    }

    /**
     * Returns the global singleton instance.
     *
     * @return the singleton
     * @throws IllegalStateException if not yet initialized
     */
    public static @NotNull TransformerRegistry instance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("TransformerRegistry has not been initialized");
        }
        return INSTANCE;
    }

    /**
     * Registers a code transformer.
     *
     * @param transformer the transformer to register
     */
    public void addTransformer(@NotNull CodeTransformer transformer) {
        transformers.add(transformer);
    }

    /**
     * Removes a registered transformer by tag.
     *
     * <p>Any transformer whose {@link CodeTransformer#tags()} list contains
     * {@code tag} is removed. Has no effect if no match is found.
     *
     * @param tag the tag to match
     */
    public void removeTransformer(@NotNull String tag) {
        transformers.removeIf(t -> {
            List<String> tags = t.tags();
            return tags != null && tags.contains(tag);
        });
    }

    /**
     * Runs all registered transformers against the given JavaBuilder.
     *
     * <p>Each transformer receives a {@link TransformContext} containing all lines.
     * Modifications (removals, replacements, insertions) are collected per
     * transformer and applied after the transformer returns, respecting tag ownership.
     *
     * <p>The process repeats in a fixed point loop so that modifications in one
     * pass can enable further modifications by other transformers in subsequent passes.
     *
     * @param builder the Java builder to transform
     * @param codegen the class-level metadata for the script being transformed
     */
    public void transform(@NotNull JavaBuilder builder, @NotNull CodegenAccess codegen) {
        if (transformers.isEmpty()) {
            return;
        }

        List<CodeTransformer> snapshot = List.copyOf(transformers);

        boolean changed = true;
        int pass = 0;
        while (changed) {
            if (++pass > MAX_PASSES) {
                LumenLogger.warning("[CodeTransform] Reached " + MAX_PASSES + " transformation passes, aborting to prevent an infinite loop");
                break;
            }
            changed = false;

            for (CodeTransformer transformer : snapshot) {
                List<String> lines = builder.lines();
                Map<Integer, String> tags = builder.tagMap();
                Map<Integer, JavaBuilder.ScriptLineInfo> lineMap = builder.lineMap();

                JavaBuilder.ScriptLineInfo[] resolvedInfo = new JavaBuilder.ScriptLineInfo[lines.size()];
                JavaBuilder.ScriptLineInfo last = null;
                for (int i = 0; i < lines.size(); i++) {
                    JavaBuilder.ScriptLineInfo direct = lineMap.get(i);
                    if (direct != null) last = direct;
                    resolvedInfo[i] = last;
                }

                List<TaggedLine> lineSnapshot = new ArrayList<>(lines.size());
                for (int i = 0; i < lines.size(); i++) {
                    JavaBuilder.ScriptLineInfo info = resolvedInfo[i];
                    int scriptLine = info != null ? info.line() : -1;
                    String scriptSource = info != null ? info.source() : null;
                    lineSnapshot.add(new TaggedLineImpl(lines.get(i), tags.get(i), i, scriptLine, scriptSource));
                }

                TransformContextImpl ctx = new TransformContextImpl(Collections.unmodifiableList(lineSnapshot), codegen, builder);
                transformer.transform(ctx);

                if (applyModifications(builder, transformer.tags(), ctx)) {
                    changed = true;
                }
            }
        }
    }

    private static boolean applyModifications(@NotNull JavaBuilder builder,
                                              @Nullable List<String> transformerTags,
                                              @NotNull TransformContextImpl ctx) {
        List<String> lines = builder.lines();
        Map<Integer, String> tags = builder.tagMap();
        boolean modified = false;

        for (int idx : ctx.replacements.keySet()) {
            if (idx < 0 || idx >= lines.size()) continue;
            if (!ownsLine(transformerTags, tags.get(idx))) continue;
            lines.set(idx, ctx.replacements.get(idx));
            modified = true;
        }

        List<Integer> removals = new ArrayList<>(ctx.removals);
        removals.removeIf(idx -> idx < 0 || idx >= lines.size() || !ownsLine(transformerTags, tags.get(idx)));

        if (!removals.isEmpty()) {
            removals.sort(Collections.reverseOrder());
            for (int idx : removals) {
                lines.remove(idx);
                shiftMapsAfterRemove(builder, tags, idx);
            }
            modified = true;
            LumenLogger.debug("CodeTransform", "Removed " + removals.size() + " line(s)");
        }

        String insertTag = transformerTags != null && transformerTags.size() == 1 ? transformerTags.get(0) : null;

        TreeMap<Integer, List<Insertion>> sortedInsertions = new TreeMap<>(Collections.reverseOrder());
        for (Insertion ins : ctx.insertions) {
            sortedInsertions.computeIfAbsent(ins.anchor, k -> new ArrayList<>()).add(ins);
        }
        for (var entry : sortedInsertions.entrySet()) {
            for (Insertion ins : entry.getValue()) {
                int target = ins.before ? ins.anchor : ins.anchor + 1;
                if (target < 0) target = 0;
                if (target > lines.size()) target = lines.size();
                lines.add(target, ins.code);

                shiftMapsAfterInsert(builder, tags, target);
                if (insertTag != null) {
                    tags.put(target, insertTag);
                }
                modified = true;
            }
        }

        return modified;
    }

    private static boolean ownsLine(@Nullable List<String> transformerTags, @Nullable String lineTag) {
        if (transformerTags == null) {
            return true;
        }
        if (transformerTags.isEmpty()) {
            return lineTag != null;
        }
        return lineTag != null && transformerTags.contains(lineTag);
    }

    private static void shiftMapsAfterRemove(@NotNull JavaBuilder builder,
                                             @NotNull Map<Integer, String> tags,
                                             int idx) {
        Map<Integer, JavaBuilder.ScriptLineInfo> lineMap = builder.lineMap();
        Map<Integer, JavaBuilder.ScriptLineInfo> shifted = new HashMap<>();
        for (var entry : lineMap.entrySet()) {
            int key = entry.getKey();
            if (key == idx) continue;
            shifted.put(key > idx ? key - 1 : key, entry.getValue());
        }
        lineMap.clear();
        lineMap.putAll(shifted);

        Map<Integer, String> newTags = new HashMap<>();
        for (var entry : tags.entrySet()) {
            int key = entry.getKey();
            if (key == idx) continue;
            newTags.put(key > idx ? key - 1 : key, entry.getValue());
        }
        tags.clear();
        tags.putAll(newTags);
    }

    private static void shiftMapsAfterInsert(@NotNull JavaBuilder builder,
                                             @NotNull Map<Integer, String> tags,
                                             int idx) {
        Map<Integer, JavaBuilder.ScriptLineInfo> lineMap = builder.lineMap();
        Map<Integer, JavaBuilder.ScriptLineInfo> shifted = new HashMap<>();
        for (var entry : lineMap.entrySet()) {
            int key = entry.getKey();
            shifted.put(key >= idx ? key + 1 : key, entry.getValue());
        }
        lineMap.clear();
        lineMap.putAll(shifted);

        Map<Integer, String> newTags = new HashMap<>();
        for (var entry : tags.entrySet()) {
            int key = entry.getKey();
            newTags.put(key >= idx ? key + 1 : key, entry.getValue());
        }
        tags.clear();
        tags.putAll(newTags);
    }

    private record TaggedLineImpl(@NotNull String code, @Nullable String tag, int index, int scriptLine, @Nullable String scriptSource) implements TaggedLine {
    }

    private static final class TransformContextImpl implements TransformContext {
        private final List<TaggedLine> snapshot;
        private final CodegenAccess codegen;
        private final JavaBuilder builder;
        private final List<Integer> removals = new ArrayList<>();
        private final Map<Integer, String> replacements = new HashMap<>();
        private final List<Insertion> insertions = new ArrayList<>();
        private String cachedFullSource;
        private int cachedBodyOffset = -1;

        private TransformContextImpl(@NotNull List<TaggedLine> snapshot, @NotNull CodegenAccess codegen, @NotNull JavaBuilder builder) {
            this.snapshot = snapshot;
            this.codegen = codegen;
            this.builder = builder;
        }

        @Override
        public @NotNull CodegenAccess codegen() {
            return codegen;
        }

        @Override
        public @NotNull List<TaggedLine> lines() {
            return snapshot;
        }

        @Override
        public @NotNull String fullSource() {
            buildIfNeeded();
            return cachedFullSource;
        }

        @Override
        public int indexOfFullSourceLine(int fullSourceLine) {
            buildIfNeeded();
            int idx = fullSourceLine - 1 - cachedBodyOffset;
            if (idx < 0 || idx >= snapshot.size()) return -1;
            return idx;
        }

        private void buildIfNeeded() {
            if (cachedFullSource != null) return;
            CodegenContext ctx = (CodegenContext) codegen;
            cachedFullSource = ClassBuilder.buildClass(ctx.className(), ctx, builder, false);
            cachedBodyOffset = builder.lines().isEmpty() ? 0 : preambleLineCount(cachedFullSource, builder.lines().get(0));
        }

        private static int preambleLineCount(@NotNull String fullSource, @NotNull String firstBodyLine) {
            int idx = fullSource.indexOf("\n" + firstBodyLine + "\n");
            if (idx < 0) return 0;
            int count = 0;
            for (int i = 0; i <= idx; i++) if (fullSource.charAt(i) == '\n') count++;
            return count;
        }

        @Override
        public void remove(int index) {
            removals.add(index);
        }

        @Override
        public void replace(int index, @NotNull String newCode) {
            replacements.put(index, newCode);
        }

        @Override
        public void insertBefore(int index, @NotNull String code) {
            insertions.add(new Insertion(index, code, true));
        }

        @Override
        public void insertAfter(int index, @NotNull String code) {
            insertions.add(new Insertion(index, code, false));
        }

        @Override
        public void insertLinesBefore(int index, @NotNull List<String> lines) {
            for (int i = lines.size() - 1; i >= 0; i--) {
                insertions.add(new Insertion(index, lines.get(i), true));
            }
        }

        @Override
        public void insertLinesAfter(int index, @NotNull List<String> lines) {
            for (int i = lines.size() - 1; i >= 0; i--) {
                insertions.add(new Insertion(index, lines.get(i), false));
            }
        }
    }

    private record Insertion(int anchor, @NotNull String code, boolean before) {
    }
}
