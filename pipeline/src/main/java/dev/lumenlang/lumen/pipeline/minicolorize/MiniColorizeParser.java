package dev.lumenlang.lumen.pipeline.minicolorize;

import dev.lumenlang.lumen.pipeline.minicolorize.node.Node;
import dev.lumenlang.lumen.pipeline.minicolorize.node.TagNode;
import dev.lumenlang.lumen.pipeline.minicolorize.node.TextNode;
import dev.lumenlang.lumen.pipeline.minicolorize.tag.Tag;
import dev.lumenlang.lumen.pipeline.minicolorize.tag.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parses MiniColorize formatted strings into a tree of {@link Node}s.
 *
 * <p>Tag resolution is fully driven by the provided {@link TagResolver} list,
 * so the parser itself has no hardcoded knowledge of any tag types. This means
 * custom tags can be added without modifying the parser.
 */
public final class MiniColorizeParser {

    private final String input;
    private final List<TagResolver> resolvers;
    private int pos;

    private MiniColorizeParser(@NotNull String input, @NotNull List<TagResolver> resolvers) {
        this.input = input;
        this.resolvers = resolvers;
        this.pos = 0;
    }

    /**
     * Parses a MiniColorize formatted string into a list of nodes using the given resolvers.
     *
     * @param input     the input string
     * @param resolvers the tag resolvers to use
     * @return the parsed node list (never null, may be empty)
     */
    public static @NotNull List<Node> parse(@NotNull String input, @NotNull List<TagResolver> resolvers) {
        MiniColorizeParser parser = new MiniColorizeParser(input, resolvers);
        return parser.parseNodes(null);
    }

    private @NotNull List<Node> parseNodes(@Nullable String closingTag) {
        List<Node> nodes = new ArrayList<>();
        StringBuilder textBuf = new StringBuilder();

        while (pos < input.length()) {
            char c = input.charAt(pos);

            if (c == '\\' && pos + 1 < input.length()) {
                char next = input.charAt(pos + 1);
                if (next == '<' || next == '>' || next == '\\') {
                    textBuf.append(next);
                    pos += 2;
                    continue;
                }
            }

            if (c == '<') {
                int tagStart = pos;
                ParsedTag parsed = tryParseTag();

                if (parsed != null) {
                    if (parsed.closing) {
                        if (closingTag != null && matchesClosingTag(closingTag, parsed.name)) {
                            flushText(nodes, textBuf);
                            return nodes;
                        }
                        textBuf.append(input, tagStart, pos);
                        continue;
                    }

                    Tag tag = resolveTag(parsed.name, parsed.negated);
                    if (tag != null) {
                        flushText(nodes, textBuf);

                        if (tag.selfClosing()) {
                            nodes.add(new TagNode(tag, List.of()));
                        } else {
                            List<Node> children = parseNodes(parsed.name);
                            nodes.add(new TagNode(tag, children));
                        }
                        continue;
                    }

                    textBuf.append(input, tagStart, pos);
                    continue;
                }

                textBuf.append(c);
                pos++;
                continue;
            }

            textBuf.append(c);
            pos++;
        }

        flushText(nodes, textBuf);
        return nodes;
    }

    private @Nullable ParsedTag tryParseTag() {
        if (pos >= input.length() || input.charAt(pos) != '<') {
            return null;
        }

        int start = pos + 1;
        int end = input.indexOf('>', start);
        if (end < 0) {
            return null;
        }

        String content = input.substring(start, end).trim();
        if (content.isEmpty()) {
            return null;
        }

        pos = end + 1;

        boolean closing = false;
        boolean negated = false;

        if (content.startsWith("/")) {
            closing = true;
            content = content.substring(1).trim();
        } else if (content.startsWith("!")) {
            negated = true;
            content = content.substring(1).trim();
        }

        return new ParsedTag(content, closing, negated);
    }

    private boolean matchesClosingTag(@NotNull String openName, @NotNull String closeName) {
        String openBase = extractBaseTag(openName);
        String closeBase = extractBaseTag(closeName);
        return openBase.equals(closeBase);
    }

    private @NotNull String extractBaseTag(@NotNull String tagName) {
        String name = tagName;
        if (name.startsWith("!")) {
            name = name.substring(1);
        }
        int colon = name.indexOf(':');
        if (colon >= 0) {
            return name.substring(0, colon).toLowerCase(Locale.ROOT);
        }
        return name.toLowerCase(Locale.ROOT);
    }

    private @Nullable Tag resolveTag(@NotNull String name, boolean negated) {
        for (TagResolver resolver : resolvers) {
            Tag tag = resolver.resolve(name, negated);
            if (tag != null) {
                return tag;
            }
        }
        return null;
    }

    private void flushText(@NotNull List<Node> nodes, @NotNull StringBuilder buf) {
        if (!buf.isEmpty()) {
            nodes.add(new TextNode(buf.toString()));
            buf.setLength(0);
        }
    }

    private record ParsedTag(@NotNull String name, boolean closing, boolean negated) {
    }
}
