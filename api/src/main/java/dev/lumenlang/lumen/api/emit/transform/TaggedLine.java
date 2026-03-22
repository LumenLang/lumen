package dev.lumenlang.lumen.api.emit.transform;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A line of emitted Java code with an optional ownership tag.
 *
 * <p>Tags identify which system emitted the line. Code transformers use
 * the tag to determine which lines they own and whether those lines can
 * safely be modified or removed.
 */
public interface TaggedLine {

    /**
     * Returns the Java source line.
     *
     * @return the code
     */
    @NotNull String code();

    /**
     * Returns the tag that identifies the system that emitted this line,
     * or {@code null} if the line has no tag.
     *
     * @return the ownership tag, or null
     */
    @Nullable String tag();

    /**
     * Returns the 0-based index of this line in the output.
     *
     * @return the line index
     */
    int index();
}
