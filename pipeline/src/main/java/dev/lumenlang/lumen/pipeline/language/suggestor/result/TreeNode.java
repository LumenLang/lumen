package dev.lumenlang.lumen.pipeline.language.suggestor.result;

import dev.lumenlang.lumen.pipeline.language.simulator.result.Position;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * One node in the playground breakdown tree.
 *
 * @param label      short label
 * @param children   nested nodes
 * @param position   the {@link Position} this node describes, or {@code null} for root/group
 *                   nodes
 * @param attributes free-form key value pairs for rendering (confidence, narrowed type, etc)
 */
public record TreeNode(@NotNull String label, @NotNull List<TreeNode> children,
                       @Nullable Position position, @NotNull Map<String, Object> attributes) {
}
