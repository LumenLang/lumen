package net.vansencool.lumen.pipeline.minicolorize;

import net.vansencool.lumen.pipeline.minicolorize.node.Node;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Converts a list of parsed MiniColorize {@link Node}s into a platform-specific
 * result of type {@code T}.
 *
 * <p>Each target platform (Bukkit, etc.) provides its own implementation of this
 * interface to produce the appropriate formatted output.
 *
 * @param <T> the output type (e.g. {@code BaseComponent[]} for Bukkit, {@code String} for plaintext)
 */
public interface MiniColorizeSerializer<T> {

    /**
     * Serializes a list of parsed nodes into the target format.
     *
     * @param nodes the parsed node tree
     * @return the serialized result
     */
    @NotNull T serialize(@NotNull List<Node> nodes);
}
