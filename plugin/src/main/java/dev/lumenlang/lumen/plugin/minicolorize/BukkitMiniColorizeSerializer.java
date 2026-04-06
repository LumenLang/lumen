package dev.lumenlang.lumen.plugin.minicolorize;

import dev.lumenlang.lumen.pipeline.minicolorize.MiniColorize;
import dev.lumenlang.lumen.pipeline.minicolorize.MiniColorizeSerializer;
import dev.lumenlang.lumen.pipeline.minicolorize.node.Node;
import dev.lumenlang.lumen.pipeline.minicolorize.node.TagNode;
import dev.lumenlang.lumen.pipeline.minicolorize.node.TextNode;
import dev.lumenlang.lumen.pipeline.minicolorize.tag.ClickTag;
import dev.lumenlang.lumen.pipeline.minicolorize.tag.ColorTag;
import dev.lumenlang.lumen.pipeline.minicolorize.tag.DecorationTag;
import dev.lumenlang.lumen.pipeline.minicolorize.tag.GradientTag;
import dev.lumenlang.lumen.pipeline.minicolorize.tag.HoverTag;
import dev.lumenlang.lumen.pipeline.minicolorize.tag.InsertionTag;
import dev.lumenlang.lumen.pipeline.minicolorize.tag.KeybindTag;
import dev.lumenlang.lumen.pipeline.minicolorize.tag.RainbowTag;
import dev.lumenlang.lumen.pipeline.minicolorize.tag.ResetTag;
import dev.lumenlang.lumen.pipeline.minicolorize.tag.Tag;
import dev.lumenlang.lumen.pipeline.minicolorize.tag.TransitionTag;
import dev.lumenlang.lumen.pipeline.minicolorize.tag.TranslatableTag;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.KeybindComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.chat.hover.content.Entity;
import net.md_5.bungee.api.chat.hover.content.Item;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Serializes a MiniColorize node tree into Spigot {@link BaseComponent} arrays
 * using the BungeeCord Chat API.
 *
 * <p>Supports colors, decorations, click events, hover events, keybinds,
 * translatable components, insertion, rainbow, gradient, and transition effects.
 */
public final class BukkitMiniColorizeSerializer implements MiniColorizeSerializer<BaseComponent[]> {

    private static final MiniColorize HOVER_MINI_COLORIZE = MiniColorize.standard();

    private static final Map<String, ChatColor> NAMED_COLORS = Map.ofEntries(
            Map.entry("black", ChatColor.BLACK),
            Map.entry("dark_blue", ChatColor.DARK_BLUE),
            Map.entry("dark_green", ChatColor.DARK_GREEN),
            Map.entry("dark_aqua", ChatColor.DARK_AQUA),
            Map.entry("dark_red", ChatColor.DARK_RED),
            Map.entry("dark_purple", ChatColor.DARK_PURPLE),
            Map.entry("gold", ChatColor.GOLD),
            Map.entry("gray", ChatColor.GRAY),
            Map.entry("grey", ChatColor.GRAY),
            Map.entry("dark_gray", ChatColor.DARK_GRAY),
            Map.entry("dark_grey", ChatColor.DARK_GRAY),
            Map.entry("blue", ChatColor.BLUE),
            Map.entry("green", ChatColor.GREEN),
            Map.entry("aqua", ChatColor.AQUA),
            Map.entry("red", ChatColor.RED),
            Map.entry("light_purple", ChatColor.LIGHT_PURPLE),
            Map.entry("yellow", ChatColor.YELLOW),
            Map.entry("white", ChatColor.WHITE)
    );

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    @Override
    public @NotNull BaseComponent @NotNull [] serialize(@NotNull List<Node> nodes) {
        List<BaseComponent> components = new ArrayList<>();
        serializeNodes(nodes, components, new FormatState());
        return components.toArray(new BaseComponent[0]);
    }

    private void serializeNodes(@NotNull List<Node> nodes, @NotNull List<BaseComponent> out, @NotNull FormatState state) {
        for (Node node : nodes) {
            if (node instanceof TextNode textNode) {
                TextComponent tc = new TextComponent(textNode.text());
                applyState(tc, state);
                out.add(tc);
            } else if (node instanceof TagNode tagNode) {
                serializeTagNode(tagNode, out, state);
            }
        }
    }

    private void serializeTagNode(@NotNull TagNode tagNode, @NotNull List<BaseComponent> out, @NotNull FormatState parentState) {
        Tag tag = tagNode.tag();

        if (tag instanceof ResetTag) {
            serializeNodes(tagNode.children(), out, new FormatState());
        } else if (tag instanceof ColorTag colorTag) {
            FormatState childState = parentState.copy();
            childState.color = resolveColor(colorTag.color());
            serializeNodes(tagNode.children(), out, childState);
        } else if (tag instanceof DecorationTag decorationTag) {
            FormatState childState = parentState.copy();
            applyDecoration(childState, decorationTag);
            serializeNodes(tagNode.children(), out, childState);
        } else if (tag instanceof ClickTag clickTag) {
            List<BaseComponent> children = new ArrayList<>();
            serializeNodes(tagNode.children(), children, parentState);
            ClickEvent clickEvent = new ClickEvent(resolveClickAction(clickTag.action()), clickTag.value());
            for (BaseComponent child : children) {
                child.setClickEvent(clickEvent);
            }
            out.addAll(children);
        } else if (tag instanceof HoverTag hoverTag) {
            List<BaseComponent> children = new ArrayList<>();
            serializeNodes(tagNode.children(), children, parentState);
            HoverEvent hoverEvent = resolveHoverEvent(hoverTag);
            if (hoverEvent != null) {
                for (BaseComponent child : children) {
                    child.setHoverEvent(hoverEvent);
                }
            }
            out.addAll(children);
        } else if (tag instanceof KeybindTag keybindTag) {
            KeybindComponent kc = new KeybindComponent(keybindTag.key());
            applyState(kc, parentState);
            out.add(kc);
        } else if (tag instanceof TranslatableTag translatableTag) {
            List<BaseComponent> translationArgs = new ArrayList<>();
            for (String arg : translatableTag.args()) {
                translationArgs.add(new TextComponent(arg));
            }
            TranslatableComponent tc = new TranslatableComponent(
                    translatableTag.key(),
                    (Object) translationArgs.toArray(new BaseComponent[0])
            );
            applyState(tc, parentState);
            out.add(tc);
        } else if (tag instanceof InsertionTag insertionTag) {
            List<BaseComponent> children = new ArrayList<>();
            serializeNodes(tagNode.children(), children, parentState);
            for (BaseComponent child : children) {
                child.setInsertion(insertionTag.text());
            }
            out.addAll(children);
        } else if (tag instanceof RainbowTag rainbowTag) {
            String plainText = extractPlainText(tagNode.children());
            serializeRainbow(plainText, rainbowTag, out, parentState);
        } else if (tag instanceof GradientTag gradientTag) {
            String plainText = extractPlainText(tagNode.children());
            serializeGradient(plainText, gradientTag.colors(), gradientTag.phase(), out, parentState);
        } else if (tag instanceof TransitionTag transitionTag) {
            String plainText = extractPlainText(tagNode.children());
            ChatColor transitionColor = computeTransitionColor(transitionTag.colors(), transitionTag.phase());
            FormatState childState = parentState.copy();
            childState.color = transitionColor;
            TextComponent tc = new TextComponent(plainText);
            applyState(tc, childState);
            out.add(tc);
        } else {
            serializeNodes(tagNode.children(), out, parentState);
        }
    }

    private void serializeRainbow(@NotNull String text, @NotNull RainbowTag tag, @NotNull List<BaseComponent> out, @NotNull FormatState parentState) {
        if (text.isEmpty()) return;

        int len = text.length();
        for (int i = 0; i < len; i++) {
            float progress = (float) i / Math.max(len - 1, 1);
            if (tag.reversed()) {
                progress = 1f - progress;
            }

            float hue = (progress + tag.phase()) % 1f;
            if (hue < 0) hue += 1f;

            Color awtColor = Color.getHSBColor(hue, 1f, 1f);
            ChatColor color = ChatColor.of(new Color(awtColor.getRGB()));

            FormatState charState = parentState.copy();
            charState.color = color;

            TextComponent tc = new TextComponent(String.valueOf(text.charAt(i)));
            applyState(tc, charState);
            out.add(tc);
        }
    }

    private void serializeGradient(@NotNull String text, @NotNull List<String> colorNames, float phase, @NotNull List<BaseComponent> out, @NotNull FormatState parentState) {
        if (text.isEmpty()) return;

        List<Color> colors = new ArrayList<>();
        for (String name : colorNames) {
            colors.add(resolveAwtColor(name));
        }

        if (colors.size() < 2) {
            colors.add(Color.WHITE);
        }

        int len = text.length();
        for (int i = 0; i < len; i++) {
            float progress = (float) i / Math.max(len - 1, 1) + phase;
            progress = ((progress % 1f) + 1f) % 1f;

            Color interpolated = interpolateGradient(colors, progress);
            ChatColor cc = ChatColor.of(interpolated);

            FormatState charState = parentState.copy();
            charState.color = cc;

            TextComponent tc = new TextComponent(String.valueOf(text.charAt(i)));
            applyState(tc, charState);
            out.add(tc);
        }
    }

    private @NotNull ChatColor computeTransitionColor(@NotNull List<String> colorNames, float phase) {
        List<Color> colors = new ArrayList<>();
        for (String name : colorNames) {
            colors.add(resolveAwtColor(name));
        }
        if (colors.size() < 2) {
            return ChatColor.WHITE;
        }

        float p = ((phase % 1f) + 1f) % 1f;
        Color result = interpolateGradient(colors, p);
        return ChatColor.of(result);
    }

    private @NotNull Color interpolateGradient(@NotNull List<Color> colors, float progress) {
        if (colors.size() == 1) return colors.get(0);

        float scaled = progress * (colors.size() - 1);
        int index = Math.min((int) scaled, colors.size() - 2);
        float local = scaled - index;

        Color from = colors.get(index);
        Color to = colors.get(index + 1);

        int r = (int) (from.getRed() + (to.getRed() - from.getRed()) * local);
        int g = (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * local);
        int b = (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * local);

        return new Color(clamp(r), clamp(g), clamp(b));
    }

    private @NotNull Color resolveAwtColor(@NotNull String name) {
        ChatColor cc = resolveColor(name);
        if (cc == null) {
            return Color.WHITE;
        }
        return cc.getColor();
    }

    private @NotNull String extractPlainText(@NotNull List<Node> nodes) {
        StringBuilder sb = new StringBuilder();
        for (Node node : nodes) {
            if (node instanceof TextNode textNode) {
                sb.append(textNode.text());
            } else if (node instanceof TagNode tagNode) {
                sb.append(extractPlainText(tagNode.children()));
            }
        }
        return sb.toString();
    }

    private @Nullable ChatColor resolveColor(@NotNull String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        ChatColor named = NAMED_COLORS.get(lower);
        if (named != null) {
            return named;
        }

        if (lower.startsWith("#") && lower.length() == 7) {
            try {
                return ChatColor.of(lower);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        return null;
    }

    private @NotNull ClickEvent.Action resolveClickAction(@NotNull String action) {
        return switch (action) {
            case "open_url" -> ClickEvent.Action.OPEN_URL;
            case "run_command" -> ClickEvent.Action.RUN_COMMAND;
            case "copy_to_clipboard" -> ClickEvent.Action.COPY_TO_CLIPBOARD;
            case "change_page" -> ClickEvent.Action.CHANGE_PAGE;
            default -> ClickEvent.Action.SUGGEST_COMMAND;
        };
    }

    private @Nullable HoverEvent resolveHoverEvent(@NotNull HoverTag hoverTag) {
        return switch (hoverTag.action()) {
            case "show_text" -> new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(HOVER_MINI_COLORIZE.process(hoverTag.value(), this)));
            case "show_item" -> resolveShowItem(hoverTag.value());
            case "show_entity" -> resolveShowEntity(hoverTag.value());
            default -> null;
        };
    }

    private @NotNull HoverEvent resolveShowItem(@NotNull String value) {
        String id = value;
        int count = 1;
        int lastColon = value.lastIndexOf(':');
        if (lastColon >= 0) {
            String suffix = value.substring(lastColon + 1);
            try {
                count = Integer.parseInt(suffix);
                id = value.substring(0, lastColon);
            } catch (NumberFormatException ignored) {
            }
        }
        if (!id.contains(":")) {
            id = "minecraft:" + id;
        }
        return new HoverEvent(HoverEvent.Action.SHOW_ITEM, new Item(id, count, null));
    }

    private @Nullable HoverEvent resolveShowEntity(@NotNull String value) {
        String[] parts = value.split(":");
        if (parts.length < 2) {
            return null;
        }
        String type;
        String uuid;
        String name = null;
        if (parts.length == 2) {
            type = "minecraft:" + parts[0];
            uuid = parts[1];
        } else if (parts.length == 3) {
            type = "minecraft:" + parts[0];
            uuid = parts[1];
            name = parts[2];
        } else {
            type = parts[0] + ":" + parts[1];
            uuid = parts[2];
            name = value.substring(parts[0].length() + 1 + parts[1].length() + 1 + parts[2].length() + 1);
        }
        return new HoverEvent(HoverEvent.Action.SHOW_ENTITY, new Entity(type, uuid, name != null ? new TextComponent(name) : null));
    }

    private void applyState(@NotNull BaseComponent component, @NotNull FormatState state) {
        if (state.color != null) component.setColor(state.color);
        if (state.bold != null) component.setBold(state.bold);
        if (state.italic != null) component.setItalic(state.italic);
        if (state.underlined != null) component.setUnderlined(state.underlined);
        if (state.strikethrough != null) component.setStrikethrough(state.strikethrough);
        if (state.obfuscated != null) component.setObfuscated(state.obfuscated);
    }

    private void applyDecoration(@NotNull FormatState state, @NotNull DecorationTag tag) {
        switch (tag.decoration()) {
            case "bold" -> state.bold = tag.enabled();
            case "italic" -> state.italic = tag.enabled();
            case "underlined" -> state.underlined = tag.enabled();
            case "strikethrough" -> state.strikethrough = tag.enabled();
            case "obfuscated" -> state.obfuscated = tag.enabled();
        }
    }

    private static final class FormatState {
        private ChatColor color;
        private Boolean bold;
        private Boolean italic;
        private Boolean underlined;
        private Boolean strikethrough;
        private Boolean obfuscated;

        private @NotNull FormatState copy() {
            FormatState copy = new FormatState();
            copy.color = this.color;
            copy.bold = this.bold;
            copy.italic = this.italic;
            copy.underlined = this.underlined;
            copy.strikethrough = this.strikethrough;
            copy.obfuscated = this.obfuscated;
            return copy;
        }
    }
}
