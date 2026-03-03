---
description: "Rich text formatting with tags for colors, gradients, rainbows, click events, hover tooltips, and more."
---

# MiniColorize

MiniColorize is Lumen's built-in text formatting system. It uses a tag-based syntax for colors, decorations, click events, hover tooltips, gradients, rainbows, and more. It is always active and works automatically in any text Lumen sends to players.

## Basic Colors

Use a color tag to apply a color. The color applies to all text that follows it until another color is used or the string ends. Closing tags are not required.

```luma
message player "<red>This is red text"
message player "<gold>Gold text <green>and now green"
```

You can close tags if you want, but it is entirely optional:

```luma
message player "<gold>Gold text <green>green part</green> back to gold"
```

### Named Colors

`black`, `dark_blue`, `dark_green`, `dark_aqua`, `dark_red`, `dark_purple`, `gold`, `gray`, `grey`, `dark_gray`, `dark_grey`, `blue`, `green`, `aqua`, `red`, `light_purple`, `yellow`, `white`

### Hex Colors

Use `<#RRGGBB>` for any custom color:

```luma
message player "<#FF5555>Custom red text"
```

You can also write `<color:yellow>` or `<c:#FF5555>` as alternative syntax.

## Decorations

Apply text styles with decoration tags:

| Tag | Shorthand | Effect |
|---|---|---|
| `<bold>` | `<b>` | **Bold** |
| `<italic>` | `<i>`, `<em>` | *Italic* |
| `<underlined>` | `<u>` | Underline |
| `<strikethrough>` | `<st>` | Strikethrough |
| `<obfuscated>` | `<obf>` | Scrambled text |

```luma
message player "<bold>Bold text <reset>and <italic>italic text"
message player "<b><red>Bold red!"
```

### Disabling a Decoration

Use `<!bold>` or `<bold:false>` to explicitly disable a decoration:

```luma
message player "<bold>Everything bold <!bold>except this"
```

## Reset

Clear all formatting with `<reset>` or `<r>`:

```luma
message player "<red><bold>Styled<reset> Plain text"
```

## Gradients

Apply a smooth color transition across text:

```luma
message player "<gradient:#5e4fa2:#f79459>Gradient text!"
message player "<gradient:green:blue>Green to blue"
```

You can use multiple color stops:

```luma
message player "<gradient:red:yellow:green>Three color gradient"
```

## Rainbow

Apply a rainbow effect across the text:

```luma
message player "<rainbow>Rainbow colored text!"
```

Reverse it with `<rainbow:!>` or shift the starting phase with a number like `<rainbow:2>`.

## Click Events

Make text clickable:

```luma
message player "<click:run_command:/help>Click for help"
message player "<click:open_url:https://example.com>Visit site"
message player "<click:suggest_command:/msg >Click to message"
message player "<click:copy_to_clipboard:Hello!>Click to copy"
```

Available actions: `open_url`, `run_command`, `suggest_command`, `copy_to_clipboard`, `change_page`.

## Hover Events

Show text when hovering:

```luma
message player "<hover:show_text:'Extra info here'>Hover over me"
```

Other hover types: `show_item` and `show_entity`.

## Keybinds

Display a keybind that adapts to the player's settings:

```luma
message player "Press <key:key.jump> to jump!"
```

## Translatable Text

Use Minecraft's translation keys for localized text:

```luma
message player "<lang:block.minecraft.diamond_block>"
```

With a fallback for unsupported keys: `<lang_or:my.key:Fallback text>`

## Insertion

Shift-click to insert text into the player's chat box:

```luma
message player "<insert:/help>Shift-click to insert /help"
```

## Mixing with Color Codes

MiniColorize tags and `&` color codes can coexist in the same string. When color codes are enabled, `&` codes are automatically converted before MiniColorize processes the text:

:::alert note
MiniColorize is separate from `&` color codes. If color code processing is disabled in your config, `&` codes will not work, but MiniColorize tags will still function.
:::

```luma
message player "&6Gold with <bold>MiniColorize bold"
```
