package dev.lumenlang.lumen.pipeline.minicolorize.node;

import dev.lumenlang.lumen.pipeline.minicolorize.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A node representing a tagged section of text. Contains the applied tag and
 * a list of child nodes that are affected by that tag.
 *
 * @param tag      the tag applied to this section
 * @param children the child nodes within this tagged section
 */
public record TagNode(@NotNull Tag tag, @NotNull List<Node> children) implements Node {

    @Override
    public @NotNull String describe() {
        String childDesc = children.stream()
                .map(Node::describe)
                .collect(Collectors.joining(", "));
        return "tag(" + tag.describe() + ", [" + childDesc + "])";
    }
}
