package net.vansencool.lumen.pipeline.minicolorize;

import net.vansencool.lumen.pipeline.minicolorize.node.Node;
import net.vansencool.lumen.pipeline.minicolorize.tag.ClickTag;
import net.vansencool.lumen.pipeline.minicolorize.tag.ColorTag;
import net.vansencool.lumen.pipeline.minicolorize.tag.DecorationTag;
import net.vansencool.lumen.pipeline.minicolorize.tag.GradientTag;
import net.vansencool.lumen.pipeline.minicolorize.tag.HoverTag;
import net.vansencool.lumen.pipeline.minicolorize.tag.InsertionTag;
import net.vansencool.lumen.pipeline.minicolorize.tag.KeybindTag;
import net.vansencool.lumen.pipeline.minicolorize.tag.RainbowTag;
import net.vansencool.lumen.pipeline.minicolorize.tag.ResetTag;
import net.vansencool.lumen.pipeline.minicolorize.tag.TagResolver;
import net.vansencool.lumen.pipeline.minicolorize.tag.TransitionTag;
import net.vansencool.lumen.pipeline.minicolorize.tag.TranslatableTag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Main entry point for the MiniColorize system.
 *
 * <p>MiniColorize is a lightweight MiniMessage-inspired tag parser that converts
 * formatted strings like {@code <yellow>Hello <bold>world</bold>!</yellow>}
 * into a structured node tree, which can then be serialized to any platform
 * format via a {@link MiniColorizeSerializer}.
 *
 * <p>The tag system is fully extensible via {@link TagResolver}s. Use
 * {@link #builder()} to create a custom instance with only the resolvers you need,
 * or use {@link #standard()} for an instance with all built-in tags.
 *
 * <p>Usage:
 * <pre>{@code
 * MiniColorize mc = MiniColorize.standard();
 * List<Node> nodes = mc.parse("<yellow>Hello <bold>world</bold>!");
 * BaseComponent[] result = serializer.serialize(nodes);
 * }</pre>
 */
public final class MiniColorize {

    private final List<TagResolver> resolvers;

    private MiniColorize(@NotNull List<TagResolver> resolvers) {
        this.resolvers = Collections.unmodifiableList(resolvers);
    }

    /**
     * Creates a new MiniColorize instance with all built-in tag resolvers registered.
     *
     * @return a standard MiniColorize instance
     */
    public static @NotNull MiniColorize standard() {
        return builder()
                .resolver(ResetTag.resolver())
                .resolver(DecorationTag.resolver())
                .resolver(ClickTag.resolver())
                .resolver(HoverTag.resolver())
                .resolver(KeybindTag.resolver())
                .resolver(TranslatableTag.resolver())
                .resolver(InsertionTag.resolver())
                .resolver(RainbowTag.resolver())
                .resolver(GradientTag.resolver())
                .resolver(TransitionTag.resolver())
                .resolver(ColorTag.resolver())
                .build();
    }

    /**
     * Creates a new builder for constructing a MiniColorize instance with custom resolvers.
     *
     * @return a new builder
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    /**
     * Parses a MiniColorize formatted string into a tree of nodes.
     *
     * @param input the input string with MiniColorize tags
     * @return the list of parsed nodes
     */
    public @NotNull List<Node> parse(@NotNull String input) {
        return MiniColorizeParser.parse(input, resolvers);
    }

    /**
     * Parses a MiniColorize formatted string, then serializes it using the given serializer.
     *
     * @param input      the input string with MiniColorize tags
     * @param serializer the serializer to use for output
     * @param <T>        the serialization result type
     * @return the serialized result
     */
    public <T> @NotNull T process(@NotNull String input, @NotNull MiniColorizeSerializer<T> serializer) {
        List<Node> nodes = parse(input);
        return serializer.serialize(nodes);
    }

    /**
     * Builder for constructing a {@link MiniColorize} instance with custom tag resolvers.
     */
    public static final class Builder {

        private final List<TagResolver> resolvers = new ArrayList<>();

        private Builder() {
        }

        /**
         * Adds a tag resolver.
         *
         * @param resolver the resolver to add
         * @return this builder
         */
        public @NotNull Builder resolver(@NotNull TagResolver resolver) {
            resolvers.add(resolver);
            return this;
        }

        /**
         * Builds the MiniColorize instance.
         *
         * @return the configured instance
         */
        public @NotNull MiniColorize build() {
            return new MiniColorize(new ArrayList<>(resolvers));
        }
    }
}
