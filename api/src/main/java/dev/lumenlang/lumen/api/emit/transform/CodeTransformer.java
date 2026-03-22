package dev.lumenlang.lumen.api.emit.transform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A code transformer that inspects and modifies emitted Java code after
 * code generation.
 *
 * <p>Each transformer declares the tags it owns through {@link #tags()}, and
 * can remove, replace, or insert lines it owns through {@link TransformContext}.
 * The pipeline enforces ownership: modifications on lines not owned by this
 * transformer are silently discarded, so there is no need to validate tags
 * inside {@link #transform(TransformContext)}.
 *
 * <p>This is an experimental feature.
 */
public interface CodeTransformer {

    /**
     * Returns the list of tags this transformer owns, or {@code null} to
     * own all lines.
     *
     * <p>Ownership rules:
     * <ul>
     *   <li>{@code null} owns every line, tagged and untagged alike.</li>
     *   <li>Empty list owns all tagged lines (any non-null tag).</li>
     *   <li>Non-empty list owns only lines whose tag appears in this list.</li>
     * </ul>
     *
     * <p>The pipeline uses this to decide which modifications to accept when
     * {@link #transform(TransformContext)} runs. There is no need to guard
     * modifications with tag checks inside the transform method.
     *
     * @return the owned tags, or null to own all lines regardless of tag
     */
    @Nullable List<String> tags();

    /**
     * Transforms the emitted code by applying modifications through the context.
     *
     * @param ctx the transform context providing line access and modification methods
     */
    void transform(@NotNull TransformContext ctx);
}
