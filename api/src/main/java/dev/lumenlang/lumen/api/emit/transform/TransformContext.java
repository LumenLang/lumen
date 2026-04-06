package dev.lumenlang.lumen.api.emit.transform;

import dev.lumenlang.lumen.api.codegen.CodegenAccess;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Provides read and write access to the emitted lines during code transformation.
 *
 * <p>A transformer receives this context and uses it to inspect all emitted lines,
 * then apply modifications such as removing, replacing, or inserting lines.
 *
 * <p>All modifications are collected and applied after the transformer returns.
 * Line indices refer to the original snapshot passed to the transformer, so
 * modifications do not affect each other within a single pass.
 */
public interface TransformContext {

    /**
     * Returns the class-level metadata for the script being transformed.
     *
     * <p>Transformers can use this to add imports, fields, methods, or interfaces
     * to the generated class.
     *
     * @return the codegen access
     */
    @NotNull CodegenAccess codegen();

    /**
     * Returns an unmodifiable snapshot of all emitted lines.
     *
     * <p>The returned list includes both tagged and untagged lines. Indices
     * in this list correspond to the indices used by the modification methods.
     *
     * @return all emitted lines
     */
    @NotNull List<TaggedLine> lines();

    /**
     * Marks a line for removal.
     *
     * <p>The line must be owned by the calling transformer (its tag must match
     * the transformer's tag). Attempts to remove lines with a different tag
     * are silently ignored by the pipeline.
     *
     * @param index the 0-based index of the line to remove
     */
    void remove(int index);

    /**
     * Marks a line for replacement with new code.
     *
     * <p>The line must be owned by the calling transformer. The replacement
     * inherits the same tag as the original line.
     *
     * @param index   the 0-based index of the line to replace
     * @param newCode the replacement Java source line
     */
    void replace(int index, @NotNull String newCode);

    /**
     * Inserts a new line before the specified index.
     *
     * <p>If the calling transformer declares exactly one tag, the inserted
     * line is automatically tagged with that tag. Otherwise the inserted
     * line is untagged.
     *
     * @param index the 0-based index to insert before
     * @param code  the Java source line to insert
     */
    void insertBefore(int index, @NotNull String code);

    /**
     * Inserts a new line after the specified index.
     *
     * <p>If the calling transformer declares exactly one tag, the inserted
     * line is automatically tagged with that tag. Otherwise the inserted
     * line is untagged.
     *
     * @param index the 0-based index to insert after
     * @param code  the Java source line to insert
     */
    void insertAfter(int index, @NotNull String code);

    /**
     * Inserts multiple lines before the specified index.
     *
     * <p>Lines are inserted in order, so the first element ends up closest
     * to the top. If the calling transformer declares exactly one tag, all
     * inserted lines are automatically tagged with that tag. Otherwise
     * they are untagged.
     *
     * @param index the 0-based index to insert before
     * @param lines the Java source lines to insert
     */
    void insertLinesBefore(int index, @NotNull List<String> lines);

    /**
     * Inserts multiple lines after the specified index.
     *
     * <p>Lines are inserted in order, so the first element ends up immediately
     * after the anchor. If the calling transformer declares exactly one tag,
     * all inserted lines are automatically tagged with that tag. Otherwise
     * they are untagged.
     *
     * @param index the 0-based index to insert after
     * @param lines the Java source lines to insert
     */
    void insertLinesAfter(int index, @NotNull List<String> lines);
}
